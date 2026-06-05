#include "raylib.h"
#include <string>
#include <vector>
#include <cstring>
#include <cstdlib>
#include <ctime>
#include <fstream>
#include <sstream>
#include <thread>
#include <atomic>
#include <stdexcept>

// ========================================================================
// 1. CẤU HÌNH GIAO DIỆN & MÀU SẮC
// ========================================================================
const Color BG_COLOR = { 248, 249, 245, 255 };        
const Color CARD_BG = { 255, 255, 255, 255 };         
const Color TEXT_MAIN = { 80, 80, 80, 255 };          
const Color TEXT_HEADING = { 40, 120, 130, 255 };     
const Color BTN_TEAL = { 110, 180, 190, 255 };        
const Color BTN_HOVER = { 130, 200, 210, 255 };       
const Color BORDER_COLOR = { 220, 220, 210, 255 };    
const Color DANGER_COLOR = { 220, 80, 80, 255 };      

Font appFont;
Texture2D logoHaUI;
enum Tab { PARAM_GEN, KEY_SIGN, VERIFY, THEORY };

typedef unsigned long long uint64;

// ========================================================================
// 2. BIẾN TOÀN CỤC CHO ĐA LUỒNG (ASYNC PROGRESS)
// ========================================================================
std::atomic<float> asyncProgress(0.0f);
std::atomic<bool> isProcessing(false);
std::atomic<uint64> asyncHashResult(0);
std::atomic<int> asyncStatus(0);    // 0: Rảnh, 1: Xong, -1: File rỗng, -2: Lỗi đọc file
std::atomic<int> asyncOperation(0); // 1: Băm để Ký, 2: Băm để Xác minh

struct BigInt {
    uint64 val;
    BigInt() : val(0) {}
    BigInt(uint64 v) : val(v) {}
    std::string toHex() const {
        char buf[30]; snprintf(buf, sizeof(buf), "%llX", val); return std::string(buf);
    }
};

struct DSAParams { BigInt p, q, g; };
struct PrivKey { BigInt x; };
struct PubKey { BigInt y; };
struct Signature { BigInt r, s; };

struct AppState {
    DSAParams params; PrivKey privKey; PubKey pubKey; Signature signature;
    bool hasParams = false; bool hasKeys = false; bool hasSignature = false;
    std::string fileToSignPath = "";
    std::string verifyFilePath = "";
    std::string verifySigPath = "";
    std::string verifyPubKeyPath = "";
};
AppState gState;

// ========================================================================
// 3. FILE IO HELPER (ĐÃ NÂNG CẤP BẮT LỖI & ĐA LUỒNG)
// ========================================================================
namespace FileIOHelper {
    
    // Hàm băm file chạy ngầm (Không làm đơ giao diện)
    void hashFileAsync(std::string filepath) {
        isProcessing = true;
        asyncProgress = 0.0f;
        asyncHashResult = 0;

        std::ifstream file(filepath, std::ios::binary | std::ios::ate);
        if (!file.is_open()) {
            asyncStatus = -2; isProcessing = false; return;
        }

        std::streamsize size = file.tellg();
        if (size == 0) { // Chặn file rỗng 0 bytes
            asyncStatus = -1; isProcessing = false; return;
        }

        file.seekg(0, std::ios::beg);
        uint64 hash = 5381;
        char buffer[8192];
        std::streamsize bytesRead = 0;

        while (file.read(buffer, sizeof(buffer)) || file.gcount() > 0) {
            std::streamsize count = file.gcount();
            for (int i = 0; i < count; i++) {
                hash = ((hash << 5) + hash) + buffer[i];
            }
            bytesRead += count;
            asyncProgress = (float)bytesRead / size;
        }

        asyncHashResult = (hash == 0 ? 1 : hash);
        asyncStatus = 1;
        isProcessing = false;
    }

    // Xuất Khóa ra ổ cứng với tên file tự sinh theo thời gian thực (Tránh ghi đè)
    bool exportKeys(const DSAParams& p, const PrivKey& priv, const PubKey& pub, std::string& outPrivName, std::string& outPubName) {
        // Lấy thời gian hiện tại
        time_t now = time(0);
        tm* ltm = localtime(&now);
        char timeBuf[30];
        strftime(timeBuf, sizeof(timeBuf), "%Y%m%d_%H%M%S", ltm); // Định dạng: YYYYMMDD_HHMMSS

        // Gắn thời gian vào tên file
        outPrivName = std::string("private_") + timeBuf + ".key";
        outPubName = std::string("public_") + timeBuf + ".key";

        std::ofstream privFile(outPrivName);
        if (privFile.is_open()) {
            privFile << p.p.val << "\n" << p.q.val << "\n" << p.g.val << "\n" << priv.x.val;
            privFile.close();
        } else return false;

        std::ofstream pubFile(outPubName);
        if (pubFile.is_open()) {
            pubFile << p.p.val << "\n" << p.q.val << "\n" << p.g.val << "\n" << pub.y.val;
            pubFile.close();
            return true;
        }
        return false;
    }

    bool exportSignature(const std::string& originalFile, const Signature& sig) {
        std::ofstream sigFile(originalFile + ".sig");
        if (sigFile.is_open()) { sigFile << sig.r.val << "\n" << sig.s.val; sigFile.close(); return true; }
        return false;
    }

    // Nâng cấp: Đọc PublicKey khóa chặt định dạng số
    bool loadPublicKey(const std::string& path, DSAParams& p, PubKey& pub) {
        std::ifstream file(path);
        if (!file.is_open()) return false;
        std::string p_str, q_str, g_str, y_str;
        if (!(file >> p_str >> q_str >> g_str >> y_str)) return false;
        
        try {
            size_t pos; // Biến đánh dấu vị trí C++ ngừng đọc số
            p.p.val = std::stoull(p_str, &pos); if(pos != p_str.length()) return false; // Nếu chuỗi chứa chữ, pos sẽ bị ngắn hơn độ dài chuỗi -> BÁO LỖI!
            p.q.val = std::stoull(q_str, &pos); if(pos != q_str.length()) return false;
            p.g.val = std::stoull(g_str, &pos); if(pos != g_str.length()) return false;
            pub.y.val = std::stoull(y_str, &pos); if(pos != y_str.length()) return false;
            return true;
        } catch (...) {
            return false;
        }
    }

    // Nâng cấp: Đọc Chữ ký khóa chặt định dạng số
    bool loadSignature(const std::string& path, Signature& sig) {
        std::ifstream file(path);
        if (!file.is_open()) return false;
        std::string r_str, s_str;
        if (!(file >> r_str >> s_str)) return false;
        
        try {
            size_t pos;
            sig.r.val = std::stoull(r_str, &pos); if(pos != r_str.length()) return false;
            sig.s.val = std::stoull(s_str, &pos); if(pos != s_str.length()) return false;
            return true;
        } catch (...) {
            return false;
        }
    }
}

// ========================================================================
// 4. BACKEND: TOÁN HỌC MẬT MÃ (DSA CORE)
// ========================================================================
namespace MathDSA {
    uint64 mulMod(uint64 a, uint64 b, uint64 m) { return (uint64)((__int128)a * b % m); }
    uint64 powerMod(uint64 base, uint64 exp, uint64 mod) {
        uint64 res = 1; base %= mod;
        while (exp > 0) { if (exp % 2 == 1) res = mulMod(res, base, mod); base = mulMod(base, base, mod); exp /= 2; }
        return res;
    }
    bool millerTest(uint64 d, uint64 n) {
        uint64 a = 2 + rand() % (n - 4); uint64 x = powerMod(a, d, n);
        if (x == 1 || x == n - 1) return true;
        while (d != n - 1) { x = mulMod(x, x, n); d *= 2; if (x == 1) return false; if (x == n - 1) return true; }
        return false;
    }
    bool isPrime(uint64 n, int k = 5) {
        if (n <= 1 || n == 4) return false; if (n <= 3) return true; if (n % 2 == 0) return false;
        uint64 d = n - 1; while (d % 2 == 0) d /= 2;
        for (int i = 0; i < k; i++) if (!millerTest(d, n)) return false;
        return true;
    }
    uint64 modInverse(uint64 a, uint64 m) {
        long long m0 = m, y = 0, x = 1; if (m == 1) return 0; long long a_ll = a;
        while (a_ll > 1) { long long q = a_ll / m0; long long t = m0; m0 = a_ll % m0; a_ll = t; t = y; y = x - q * y; x = t; }
        if (x < 0) x += m; return x;
    }
}

namespace DSA {
    DSAParams generateParams() {
        DSAParams params; uint64 q, p, k;
        while (true) { q = (rand() % 500000) + 100000; if (MathDSA::isPrime(q)) break; }
        while (true) { k = (rand() % 50000) + 10000; p = k * q + 1; if (MathDSA::isPrime(p)) break; }
        uint64 h = 2, g;
        while (true) { g = MathDSA::powerMod(h, (p - 1) / q, p); if (g > 1) break; h++; }
        params.p = p; params.q = q; params.g = g; return params;
    }
    std::pair<PrivKey, PubKey> generateKeyPair(DSAParams p) {
        PrivKey priv; PubKey pub;
        priv.x = 1 + rand() % (p.q.val - 1);
        pub.y = MathDSA::powerMod(p.g.val, priv.x.val, p.p.val); return {priv, pub};
    }
    Signature signHash(uint64 hm, PrivKey key, DSAParams p) {
        uint64 k_rand, r, s;
        do {
            do { k_rand = 1 + rand() % (p.q.val - 1); r = MathDSA::powerMod(p.g.val, k_rand, p.p.val) % p.q.val; } while (r == 0);
            uint64 k_inv = MathDSA::modInverse(k_rand, p.q.val);
            uint64 xr = MathDSA::mulMod(key.x.val, r, p.q.val);
            uint64 hm_xr = (hm + xr) % p.q.val;
            s = MathDSA::mulMod(k_inv, hm_xr, p.q.val);
        } while (s == 0);
        Signature sig; sig.r = r; sig.s = s; return sig;
    }
    bool verifyHash(uint64 hm, Signature sig, PubKey key, DSAParams p) {
        uint64 r = sig.r.val, s = sig.s.val;
        if (r <= 0 || r >= p.q.val || s <= 0 || s >= p.q.val) return false;
        uint64 w = MathDSA::modInverse(s, p.q.val);
        uint64 u1 = MathDSA::mulMod(hm, w, p.q.val);
        uint64 u2 = MathDSA::mulMod(r, w, p.q.val);
        uint64 gu1 = MathDSA::powerMod(p.g.val, u1, p.p.val);
        uint64 yu2 = MathDSA::powerMod(key.y.val, u2, p.p.val);
        uint64 v = (MathDSA::mulMod(gu1, yu2, p.p.val)) % p.q.val;
        return v == r;
    }
}

// ========================================================================
// 5. CÁC HÀM VẼ GIAO DIỆN CHÍNH
// ========================================================================
void DrawTextBeautiful(const char* text, int x, int y, int fontSize, Color color) {
    DrawTextEx(appFont, text, {(float)x, (float)y}, (float)fontSize, 1.0f, color);
}

void DrawTextBox(int x, int y, int width, int height, const std::string& label, const std::string& text, Color textColor) {
    DrawTextBeautiful(label.c_str(), x, y - 22, 15, TEXT_HEADING);
    DrawRectangle(x, y, width, height, CARD_BG);
    DrawRectangleLines(x, y, width, height, BORDER_COLOR);
    int fontSize = 15; int posX = x + 15; int posY = y + 15; std::string line = "";
    for (size_t i = 0; i < text.size(); i++) {
        line += text[i];
        if (line.size() >= width / 8 || text[i] == '\n') {
            DrawTextBeautiful(line.c_str(), posX, posY, fontSize, textColor); posY += fontSize + 6; line = "";
        }
    }
    if (!line.empty()) DrawTextBeautiful(line.c_str(), posX, posY, fontSize, textColor);
}

bool DrawModernButton(int x, int y, int width, int height, const char* text, bool isSelected = false, Color baseCol = BTN_TEAL) {
    Vector2 mousePos = GetMousePosition();
    bool hovered = CheckCollisionPointRec(mousePos, {(float)x, (float)y, (float)width, (float)height});
    bool clicked = hovered && IsMouseButtonPressed(MOUSE_BUTTON_LEFT);
    Color col = isSelected ? baseCol : (hovered ? ColorAlpha(baseCol, 0.8f) : LIGHTGRAY);
    DrawRectangle(x, y, width, height, col);
    Vector2 textSize = MeasureTextEx(appFont, text, 16, 1.0f);
    Color txtCol = (isSelected || hovered) ? WHITE : TEXT_MAIN;
    DrawTextBeautiful(text, x + (width - textSize.x)/2, y + (height - 16)/2, 16, txtCol);
    return clicked;
}

void DrawDropZone(int x, int y, int width, int height, const std::string& label, const std::string& filePath) {
    DrawTextBeautiful(label.c_str(), x, y - 22, 15, TEXT_HEADING);
    DrawRectangle(x, y, width, height, CARD_BG);
    Color border = filePath.empty() ? LIGHTGRAY : GREEN;
    DrawRectangleLinesEx({(float)x, (float)y, (float)width, (float)height}, 2, border);
    if (filePath.empty()) {
        DrawTextBeautiful("KÉO VÀ THẢ FILE VÀO ĐÂY...", x + width/2 - 120, y + height/2 - 8, 16, GRAY);
    } else {
        std::string displayPath = filePath.length() > 60 ? "..." + filePath.substr(filePath.length() - 57) : filePath;
        DrawTextBeautiful(("Đã tải: " + displayPath).c_str(), x + 20, y + height/2 - 8, 15, DARKGREEN);
    }
}

void LoadVietnameseFont() {
    int codepoints[1500] = { 0 }; int count = 0;
    for (int i = 32; i < 127; i++) codepoints[count++] = i;
    for (int i = 0x00A0; i < 0x0259; i++) codepoints[count++] = i;
    for (int i = 0x1EA0; i < 0x1EFE; i++) codepoints[count++] = i;
    appFont = LoadFontEx("ARIAL.TTF", 32, codepoints, count);
    SetTextureFilter(appFont.texture, TEXTURE_FILTER_BILINEAR);
}

// ========================================================================
// 6. MAIN CONTROLLER
// ========================================================================
int main() {
    srand(time(NULL)); 
    InitWindow(1200, 800, "Mo phong He chu ky so DSA");
    SetTargetFPS(60);
    LoadVietnameseFont();
    logoHaUI = LoadTexture("haui_logo.png"); 

    Tab currentTab = PARAM_GEN;
    std::string statusMessage = "Hệ thống sẵn sàng. Hãy tạo tham số để bắt đầu.";
    Color statusColor = GREEN;

    while (!WindowShouldClose()) {
        if (IsFileDropped()) {
            FilePathList droppedFiles = LoadDroppedFiles();
            std::string path = droppedFiles.paths[0]; 

            if (currentTab == KEY_SIGN && !isProcessing) {
                gState.fileToSignPath = path;
                statusMessage = "Đã tải file thành công. Sẵn sàng ký!"; statusColor = BLUE;
            } 
            else if (currentTab == VERIFY && !isProcessing) {
                if (path.find(".sig") != std::string::npos) gState.verifySigPath = path;
                else if (path.find(".key") != std::string::npos) gState.verifyPubKeyPath = path;
                else gState.verifyFilePath = path;
                statusMessage = "Đã nhận diện và tải file vào vùng tương ứng."; statusColor = BLUE;
            }
            UnloadDroppedFiles(droppedFiles);
        }

        BeginDrawing();
        ClearBackground(BG_COLOR);

        // --- SIDEBAR ---
        DrawRectangle(0, 0, 280, 800, WHITE);
        DrawRectangleLines(279, 0, 1, 800, BORDER_COLOR);
        DrawTextBeautiful("MÔ PHỎNG CHỮ KÝ SỐ DSA", 20, 40, 18, TEXT_HEADING);
        
        if (!isProcessing) { // Khóa menu khi đang chạy ngầm
            if (DrawModernButton(15, 100, 250, 45, "1. Tham số Hệ thống", currentTab == PARAM_GEN)) currentTab = PARAM_GEN;
            if (DrawModernButton(15, 160, 250, 45, "2. Quản lý Khóa & Ký", currentTab == KEY_SIGN)) currentTab = KEY_SIGN;
            if (DrawModernButton(15, 220, 250, 45, "3. Xác minh Chữ ký", currentTab == VERIFY)) currentTab = VERIFY;
            if (DrawModernButton(15, 280, 250, 45, "4. Lý thuyết Cơ chế", currentTab == THEORY)) currentTab = THEORY;
        }

        if (logoHaUI.id != 0) { 
            float scale = 240.0f / logoHaUI.width; DrawTextureEx(logoHaUI, {20, 550}, 0.0f, scale, WHITE); 
            int textY = 550 + (logoHaUI.height * scale) + 15;
            DrawTextBeautiful("Nhóm 10 - HP: An toàn & BMTT", 15, textY, 16, TEXT_MAIN);
            DrawTextBeautiful("Mã lớp: IT6001.2", 15, textY + 26, 16, TEXT_MAIN);
            DrawTextBeautiful("GV hướng dẫn: TS. Phạm Văn Hiệp", 15, textY + 52, 16, TEXT_HEADING);
        }

        // --- THANH TRẠNG THÁI ---
        DrawTextBeautiful("Trạng thái:", 310, 748, 20, TEXT_MAIN);
        DrawTextBeautiful(statusMessage.c_str(), 440, 748, 20, statusColor);
        DrawRectangle(280, 735, 920, 1, BORDER_COLOR);

        // --- NỘI DUNG CHÍNH ---
        switch (currentTab) {
            case PARAM_GEN: {
                DrawTextBeautiful("KHỞI TẠO CÁC THAM SỐ HỆ THỐNG DSA (p, q, g)", 310, 30, 22, TEXT_HEADING);
                if (DrawModernButton(310, 110, 220, 40, "TẠO THAM SỐ", false, BTN_TEAL)) {
                    gState.params = DSA::generateParams();
                    gState.hasParams = true; gState.hasKeys = false; gState.hasSignature = false;
                    statusMessage = "Tạo hệ thống thành công! (p và q là số nguyên tố thỏa mãn điều kiện)."; statusColor = GREEN;
                }
                DrawTextBox(310, 200, 850, 120, "Số nguyên tố hệ thống p (Hex)", gState.hasParams ? gState.params.p.toHex() : "Chưa khởi tạo...", TEXT_MAIN);
                DrawTextBox(310, 360, 850, 70, "Số nguyên tố khóa q (Hex)", gState.hasParams ? gState.params.q.toHex() : "Chưa khởi tạo...", TEXT_MAIN);
                DrawTextBox(310, 470, 850, 120, "Phần tử sinh g (Bậc q modulo p)", gState.hasParams ? gState.params.g.toHex() : "Chưa khởi tạo...", TEXT_MAIN);
                break;
            }

            case KEY_SIGN: {
                DrawTextBeautiful("QUẢN LÝ KHÓA & KÝ FILE (DATA SIGNING)", 310, 30, 22, TEXT_HEADING);
                if (!gState.hasParams) {
                    DrawTextBeautiful("[!] Vui lòng quay lại Tab 1 để tạo tham số hệ thống trước!", 310, 80, 16, DANGER_COLOR); break;
                }

                if (DrawModernButton(310, 80, 200, 40, "SINH CẶP KHÓA", false, BTN_TEAL)) {
                    auto [priv, pub] = DSA::generateKeyPair(gState.params);
                    gState.privKey = priv; gState.pubKey = pub; gState.hasKeys = true;
                    statusMessage = "Đã tính toán xong Khóa Bí mật và Khóa Công khai."; statusColor = GREEN;
                }
                // Sự kiện khi bấm nút Lưu Khóa
                if (gState.hasKeys && DrawModernButton(530, 80, 200, 40, "LƯU KHÓA (.KEY)", false, {230, 160, 50, 255})) {
                    std::string generatedPrivName, generatedPubName;
                    if (FileIOHelper::exportKeys(gState.params, gState.privKey, gState.pubKey, generatedPrivName, generatedPubName)) {
                        statusMessage = "Đã xuất: " + generatedPrivName + " & " + generatedPubName; 
                        statusColor = GREEN;
                    } else {
                        statusMessage = "LỖI: Không thể ghi file khóa ra ổ cứng!";
                        statusColor = RED;
                    }
                }

                DrawTextBox(310, 150, 410, 50, "Khóa Bí mật x (Private Key)", gState.hasKeys ? gState.privKey.x.toHex() : "Chưa tạo...", DANGER_COLOR);
                DrawTextBox(750, 150, 410, 50, "Khóa Công khai y (Public Key)", gState.hasKeys ? gState.pubKey.y.toHex() : "Chưa tạo...", TEXT_HEADING);
                DrawDropZone(310, 250, 850, 80, "CHỌN FILE ĐỂ KÝ (Hỗ trợ mọi định dạng: .txt, .pdf, .mp4, .exe)", gState.fileToSignPath);

                if (gState.hasKeys && !gState.fileToSignPath.empty()) {
                    if (!isProcessing && asyncStatus == 0) {
                        if (DrawModernButton(310, 360, 250, 40, "THỰC HIỆN BĂM & KÝ", false, {230, 160, 50, 255})) {
                            asyncOperation = 1; // Đánh dấu luồng cho việc Ký
                            std::thread t(FileIOHelper::hashFileAsync, gState.fileToSignPath);
                            t.detach();
                        }
                    } 
                    // HIỂN THỊ PROGRESS BAR
                    else if (isProcessing && asyncOperation == 1) {
                        DrawTextBeautiful("Hệ thống đang tiến hành băm Stream...", 310, 345, 14, TEXT_MAIN);
                        DrawRectangleLines(310, 370, 500, 20, BORDER_COLOR);
                        DrawRectangle(311, 371, (int)(498 * asyncProgress.load()), 18, BTN_TEAL);
                        std::string pct = std::to_string((int)(asyncProgress.load() * 100)) + "%";
                        DrawTextBeautiful(pct.c_str(), 820, 372, 15, TEXT_MAIN);
                    } 
                    // XỬ LÝ KẾT QUẢ TỪ LUỒNG CHẠY NGẦM
                    else if (!isProcessing && asyncOperation == 1 && asyncStatus != 0) {
                        if (asyncStatus == -1) {
                            statusMessage = "LỖI TỪ CHỐI KÝ: File rỗng (0 bytes)!"; statusColor = RED;
                        } else if (asyncStatus == -2) {
                            statusMessage = "LỖI IO: Không thể mở file. File có thể bị hỏng."; statusColor = RED;
                        } else if (asyncStatus == 1) {
                            gState.signature = DSA::signHash(asyncHashResult, gState.privKey, gState.params);
                            gState.hasSignature = true;
                            FileIOHelper::exportSignature(gState.fileToSignPath, gState.signature);
                            statusMessage = "KÝ THÀNH CÔNG! Đã tạo file chữ ký (.sig) tại thư mục chứa file gốc."; statusColor = GREEN;
                        }
                        asyncStatus = 0; asyncOperation = 0; // Reset luồng
                    }
                }

                DrawTextBox(310, 450, 850, 90, "Thành phần chữ ký r (Hex)", gState.hasSignature ? gState.signature.r.toHex() : "Chưa ký...", TEXT_MAIN);
                DrawTextBox(310, 580, 850, 90, "Thành phần chữ ký s (Hex)", gState.hasSignature ? gState.signature.s.toHex() : "Chưa ký...", TEXT_MAIN);
                break;
            }

            case VERIFY: {
                DrawTextBeautiful("XÁC MINH CHỮ KÝ SỐ (VERIFICATION)", 310, 30, 22, TEXT_HEADING);

                DrawDropZone(310, 80, 850, 60, "1. FILE DỮ LIỆU GỐC (Data File)", gState.verifyFilePath);
                DrawDropZone(310, 180, 850, 60, "2. FILE CHỮ KÝ (.sig)", gState.verifySigPath);
                DrawDropZone(310, 280, 850, 60, "3. FILE KHÓA CÔNG KHAI (.key)", gState.verifyPubKeyPath);

                bool canVerify = !gState.verifyFilePath.empty() && !gState.verifySigPath.empty() && !gState.verifyPubKeyPath.empty();

                if (canVerify) {
                    if (!isProcessing && asyncStatus == 0) {
                        if (DrawModernButton(310, 360, 300, 45, "TIẾN HÀNH XÁC MINH TOÀN VẸN", false, {60, 170, 100, 255})) {
                            asyncOperation = 2; // Đánh dấu luồng cho việc Xác Minh
                            std::thread t(FileIOHelper::hashFileAsync, gState.verifyFilePath);
                            t.detach();
                        }
                    }
                    // HIỂN THỊ PROGRESS BAR XÁC MINH
                    else if (isProcessing && asyncOperation == 2) {
                        DrawRectangleLines(310, 370, 500, 20, BORDER_COLOR);
                        DrawRectangle(311, 371, (int)(498 * asyncProgress.load()), 18, {60, 170, 100, 255});
                        std::string pct = std::to_string((int)(asyncProgress.load() * 100)) + "%";
                        DrawTextBeautiful(("Đang quét file... " + pct).c_str(), 820, 372, 15, TEXT_MAIN);
                    }
                    // XỬ LÝ KẾT QUẢ XÁC MINH
                    else if (!isProcessing && asyncOperation == 2 && asyncStatus != 0) {
                        if (asyncStatus < 0) {
                            statusMessage = "LỖI IO: Không thể đọc File dữ liệu gốc (Rỗng hoặc bị hỏng)!"; statusColor = RED;
                        } else {
                            DSAParams extParams; PubKey extPub; Signature extSig;
                            if (!FileIOHelper::loadPublicKey(gState.verifyPubKeyPath, extParams, extPub) || 
                                !FileIOHelper::loadSignature(gState.verifySigPath, extSig)) {
                                statusMessage = "LỖI BẢO MẬT: File .sig hoặc .key đã bị sửa đổi, sai format dữ liệu!"; statusColor = RED;
                            } else {
                                bool ok = DSA::verifyHash(asyncHashResult, extSig, extPub, extParams);
                                if (ok) {
                                    statusMessage = "KẾT QUẢ: Chữ ký HỢP LỆ! File hoàn toàn nguyên vẹn từ lúc ký."; statusColor = GREEN;
                                } else {
                                    statusMessage = "CẢNH BÁO: CHỮ KÝ SAI! File đã bị sửa đổi hoặc Khóa không khớp."; statusColor = RED;
                                }
                            }
                        }
                        asyncStatus = 0; asyncOperation = 0;
                    }
                }

                DrawRectangle(310, 430, 850, 280, CARD_BG);
                DrawRectangleLines(310, 430, 850, 280, BORDER_COLOR);
                DrawTextBeautiful("QUY TRÌNH TOÁN HỌC XÁC THỰC (BACK-END):", 330, 450, 16, TEXT_HEADING);
                DrawTextBeautiful("- Băm file gốc để lấy mã Hash (Mô phỏng SHA-256 theo Stream).", 350, 480, 15, TEXT_MAIN);
                DrawTextBeautiful("- Tính w = s^-1 mod q", 350, 510, 15, TEXT_MAIN);
                DrawTextBeautiful("- Tính u1 = ( Hash(M) * w ) mod q  và  u2 = ( r * w ) mod q", 350, 540, 15, TEXT_MAIN);
                DrawTextBeautiful("- Tính v = [ (g^u1 * y^u2) mod p ] mod q", 350, 570, 15, TEXT_MAIN);
                
                if (statusColor.r == 255) { 
                    DrawRectangle(350, 620, 500, 60, DANGER_COLOR);
                    DrawTextBeautiful("ĐỐI SÁNH: v != r -> PHÁT HIỆN GIẢ MẠO / SỬA ĐỔI FILE", 370, 640, 16, WHITE);
                } else if (statusColor.g == 255 && statusMessage.find("KẾT QUẢ") != std::string::npos) { 
                    DrawRectangle(350, 620, 500, 60, {60, 170, 100, 255});
                    DrawTextBeautiful("ĐỐI SÁNH: v == r -> XÁC THỰC THÀNH CÔNG", 370, 640, 16, WHITE);
                }
                break;
            }

            case THEORY: {
                DrawTextBeautiful("TÓM TẮT LÝ THUYẾT HỆ CHỮ KÝ SỐ DSA", 310, 30, 22, TEXT_HEADING);
                DrawTextBeautiful("• Khác với RSA, DSA chỉ dùng để ký số bảo mật, KHÔNG dùng để mã hóa văn bản.", 310, 80, 16, TEXT_MAIN);
                DrawTextBeautiful("• Sử dụng thuật toán Băm (Hash) để nén các file dung lượng GB xuống chuỗi ngắn.", 310, 120, 16, TEXT_MAIN);
                DrawTextBeautiful("• Bất kỳ sự thay đổi nhỏ nào ở file gốc (dù 1 byte) cũng làm chữ ký bị sai lệch.", 310, 160, 16, TEXT_MAIN);
                
                DrawRectangle(310, 220, 850, 200, CARD_BG);
                DrawRectangleLines(310, 220, 850, 200, BORDER_COLOR);
                DrawTextBeautiful("Lưu ý về luồng đọc File IO (Đa luồng):", 330, 240, 16, TEXT_HEADING);
                DrawTextBeautiful("  Hệ thống sử dụng std::ifstream Binary Streaming trên Thread độc lập.", 330, 280, 15, TEXT_MAIN);
                DrawTextBeautiful("  Thay vì Load toàn bộ file vào RAM gây treo UI, hệ thống cắt file thành", 330, 310, 15, TEXT_MAIN);
                DrawTextBeautiful("  các khối 4KB để đọc liên tục, xử lý tốt các video nặng nhiều GB.", 330, 340, 15, TEXT_MAIN);
                break;
            }
        }

        EndDrawing();
    }

    UnloadTexture(logoHaUI); 
    UnloadFont(appFont);
    CloseWindow();
    return 0;
}