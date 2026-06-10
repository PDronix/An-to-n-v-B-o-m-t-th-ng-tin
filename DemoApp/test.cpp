#include "raylib.h"
#include "rlgl.h"
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
#include <algorithm>
#include <cstdint>

// --- THƯ VIỆN SỐ LỚN BOOST ---
#include <boost/multiprecision/cpp_int.hpp>
#include <boost/random/mersenne_twister.hpp>
#include <boost/random/uniform_int_distribution.hpp>
#include <boost/multiprecision/miller_rabin.hpp>

using namespace boost::multiprecision;
typedef cpp_int uint_large;
typedef unsigned long long uint64;

// ========================================================================
// 1. CẤU HÌNH GIAO DIỆN & MÀU SẮC
// ========================================================================
const Color BG_COLOR      = { 240, 244, 250, 255 };
const Color CARD_BG       = { 255, 255, 255, 255 };
const Color TEXT_MAIN     = {  32,  48,  70, 255 };
const Color TEXT_SUB      = {  96, 112, 136, 255 };
const Color TEXT_HEADING  = {  22,  70, 162, 255 };
const Color BTN_TEAL      = {  37,  99, 235, 255 };
const Color BORDER_COLOR  = { 214, 224, 240, 255 };
const Color DANGER_COLOR  = { 215,  35,  35, 255 };
const Color DANGER_LIGHT  = { 255, 232, 232, 255 };
const Color HEADER_BLUE   = {  13,  28,  86, 255 };
const Color HEADER_ACCENT = {  37,  99, 235, 255 };
const Color SUCCESS_COLOR = {   4, 142, 100, 255 };
const Color SUCCESS_LIGHT = { 232, 252, 242, 255 };
const Color WARNING_COLOR = { 210, 110,   4, 255 };
const Color WARNING_LIGHT = { 255, 248, 228, 255 };
const Color ROW_ALT       = { 246, 249, 254, 255 };
// Sidebar palette
const Color SB_BG1        = {  12,  28,  72, 255 };
const Color SB_BG2        = {  18,  44, 100, 255 };
const Color SB_ACCENT     = {  59, 130, 246, 255 };
const Color SB_TEXT       = { 186, 210, 250, 255 };

const int VW = 1280;
const int VH = 900;
const int SW = 290;
const int CX = SW + 10;
const int CW = VW - SW - 20;
const int STATUS_Y = VH - 56;

Font appFont;
Texture2D logoHaUI;
enum Tab { HOME, PARAM_GEN, KEY_SIGN, VERIFY, THEORY, TAB_LOG };
Vector2 gVirtualMouse = { 0 };

// ── Animation state ────────────────────────────────────────────────────────
float gTime         = 0.0f;   // tổng thời gian chạy
float gTabAnim[6]   = {};     // hover animation cho từng tab (0.0–1.0)
float gBtnPulse     = 0.0f;   // pulse cho nút CTA chính
float gStatusAlpha  = 1.0f;   // fade cho status bar
std::string gPrevStatusMsg = "";
float gStatusFade   = 0.0f;   // 0→1 khi status thay đổi
Tab   gPrevTab      = HOME;
float gTabSlide     = 0.0f;   // slide animation khi đổi tab
float gContentAlpha = 1.0f;   // alpha fade content khi đổi tab

// ========================================================================
// 2. ĐA LUỒNG & TRẠNG THÁI
// ========================================================================
std::atomic<float>  asyncProgress(0.0f);
std::atomic<bool>   isProcessing(false);
std::atomic<uint64> asyncHashResult(0);
std::atomic<int>    asyncStatus(0);
std::atomic<int>    asyncOperation(0); 
// asyncOperation: 1 = Hashing (Sign), 2 = Hashing (Verify), 3 = Generating Params

struct BigInt {
    uint_large val;
    BigInt() : val(0) {}
    BigInt(uint_large v) : val(v) {}
    
    std::string toHex(bool truncate = false) const {
        std::stringstream ss;
        ss << std::hex << std::uppercase << val;
        std::string s = ss.str();
        if (truncate && s.length() > 60) {
            return s.substr(0, 25) + " ... " + s.substr(s.length() - 25);
        }
        return s;
    }
};

struct DSAParams { BigInt p, q, g; };
DSAParams gTempParams; // Dùng để lưu trữ tạm khi luồng background chạy

struct PrivKey   { BigInt x; };
struct PubKey    { BigInt y; };
struct Signature { BigInt r, s; };

struct LogEntry {
    std::string timestamp, action, detail;
    bool success;
};

struct AppState {
    DSAParams params; PrivKey privKey; PubKey pubKey; Signature signature;
    bool hasParams = false, hasKeys = false, hasSignature = false;
    std::string fileToSignPath;
    std::string verifyFilePath, verifySigPath, verifyPubKeyPath;
    char textInput[512]       = ""; int textInputLen = 0;
    char verifyTextInput[512] = ""; int verifyTextInputLen = 0;
    bool signingText = false;
    int  lastVerifyResult = 0;
    uint64    lastSignedHash = 0;         
    PubKey    lastSignedPubKey;           
    Signature lastSignedSignature;        
    bool      hasLastSignedInfo = false;
    std::vector<LogEntry> logs;
    std::vector<std::string> savedKeyFiles;
    std::vector<bool>        revokedKeys;
    struct SignedDoc {
        std::string docName, sigFile, keyFile, timestamp;
    };
    std::vector<SignedDoc> signedDocs;
    int logScrollY = 0, docScrollY = 0, keyScrollY = 0;
} gState;

// ========================================================================
// HÀM KIỂM TRA CHỨNG CHỈ THU HỒI (CRL CHECK)
// ========================================================================
bool isKeyRevoked(const std::string& keyPath) {
    size_t pos = keyPath.find_last_of("/\\");
    std::string fileName = (pos != std::string::npos) ? keyPath.substr(pos + 1) : keyPath;
    for (size_t i = 0; i < gState.savedKeyFiles.size(); i++) {
        if (gState.savedKeyFiles[i] == fileName && gState.revokedKeys[i]) {
            return true;
        }
    }
    return false;
}

// ========================================================================
// 3. TIỆN ÍCH THỜI GIAN & LOG
// ========================================================================
std::string GetTimestamp() {
    time_t now = time(0); tm* t = localtime(&now);
    char buf[32]; strftime(buf, sizeof(buf), "%Y-%m-%d %H:%M:%S", t);
    return std::string(buf);
}
std::string GetTimeTag() {
    time_t now = time(0); tm* t = localtime(&now);
    char buf[20]; strftime(buf, sizeof(buf), "%Y%m%d_%H%M%S", t);
    return std::string(buf);
}
void AddLog(const std::string& action, const std::string& detail, bool ok) {
    LogEntry e; e.timestamp = GetTimestamp(); e.action = action;
    e.detail = detail; e.success = ok;
    gState.logs.push_back(e);
    if (gState.logs.size() > 300) gState.logs.erase(gState.logs.begin());
}

// ========================================================================
// 4. FILE IO (ĐÃ NÂNG CẤP BĂM FNV-1A 64-BIT SIÊU CHÍNH XÁC)
// ========================================================================
namespace FileIOHelper {

    // -----------------------------------------------------------------------
    // FNV-1a 64-bit hash toàn bộ bytes của một stream
    // -----------------------------------------------------------------------
    uint64 fnv1a64Stream(std::ifstream& f, std::streamsize sz,
                         std::atomic<float>& progress) {
        uint64 h = 14695981039346656037ULL;
        char buf[8192]; std::streamsize rd = 0;
        while (f.read(buf, sizeof(buf)) || f.gcount() > 0) {
            std::streamsize c = f.gcount();
            for (int i = 0; i < c; i++) {
                h ^= (unsigned char)buf[i];
                h *= 1099511628211ULL;
            }
            rd += c;
            if (sz > 0) progress = (float)rd / (float)sz;
        }
        return (h == 0) ? 1 : h;
    }

    // -----------------------------------------------------------------------
    // Kiểm tra file có phải DOCX/XLSX/PPTX (ZIP signature: PK\x03\x04)
    // -----------------------------------------------------------------------
    // Kiểm tra file có phải OOXML/ZIP (DOCX, XLSX, PPTX)
    bool isOOXMLFile(const std::string& path) {
        std::string low = path;
        std::transform(low.begin(), low.end(), low.begin(), ::tolower);
        auto ends = [&](const char* s){
            size_t sl = strlen(s);
            return low.size()>=sl && low.substr(low.size()-sl)==s;
        };
        if (!ends(".docx") && !ends(".xlsx") && !ends(".pptx") && !ends(".odt"))
            return false;
        std::ifstream f(path, std::ios::binary);
        if (!f.is_open()) return false;
        unsigned char m[4]={0}; f.read((char*)m,4);
        return (m[0]==0x50 && m[1]==0x4B && m[2]==0x03 && m[3]==0x04);
    }

    uint64 hashDocxStable(const std::string& path) {
        std::ifstream f(path, std::ios::binary | std::ios::ate);
        if (!f.is_open()) return 0;
        std::streamsize fsize = f.tellg();
        if (fsize < 22) return 0;

        // Bước 1: Tìm End of Central Directory (EOCD) từ cuối file lên
        // EOCD signature: 50 4B 05 06, tối thiểu 22 bytes trước EOF
        std::streamsize searchSize = std::min(fsize, (std::streamsize)65580);
        f.seekg(fsize - searchSize);
        std::vector<unsigned char> tail((size_t)searchSize);
        f.read((char*)tail.data(), searchSize);

        int eocdPos = -1;
        for (int i = (int)tail.size() - 22; i >= 0; i--) {
            if (tail[i]==0x50 && tail[i+1]==0x4B &&
                tail[i+2]==0x05 && tail[i+3]==0x06) {
                eocdPos = i; break;
            }
        }
        if (eocdPos < 0) return 0; // Không tìm thấy EOCD

        unsigned char* eocd = tail.data() + eocdPos;
        uint32_t cdSize   = (uint32_t)eocd[12] | ((uint32_t)eocd[13]<<8)
                          | ((uint32_t)eocd[14]<<16) | ((uint32_t)eocd[15]<<24);
        uint32_t cdOffset = (uint32_t)eocd[16] | ((uint32_t)eocd[17]<<8)
                          | ((uint32_t)eocd[18]<<16) | ((uint32_t)eocd[19]<<24);

        if ((uint64_t)cdOffset + cdSize > (uint64_t)fsize) return 0;

        // Bước 2: Đọc toàn bộ Central Directory
        f.seekg(cdOffset);
        std::vector<unsigned char> cd(cdSize);
        if (f.read((char*)cd.data(), cdSize).gcount() < (std::streamsize)cdSize)
            return 0;

        // Bước 3: Duyệt từng Central Directory Record, hash tên + CRC-32 + size
        uint64 h = 14695981039346656037ULL;
        auto fnv = [&](unsigned char b){ h ^= b; h *= 1099511628211ULL; };

        size_t pos = 0;
        while (pos + 46 <= (size_t)cdSize) {
            if (cd[pos]!=0x50||cd[pos+1]!=0x4B||cd[pos+2]!=0x01||cd[pos+3]!=0x02)
                break;
            // CRC-32 ở offset 16 trong CDR — ổn định
            uint32_t crc32    = (uint32_t)cd[pos+16]|((uint32_t)cd[pos+17]<<8)
                              | ((uint32_t)cd[pos+18]<<16)|((uint32_t)cd[pos+19]<<24);
            // Uncompressed size ở offset 24
            uint32_t uncompSz = (uint32_t)cd[pos+24]|((uint32_t)cd[pos+25]<<8)
                              | ((uint32_t)cd[pos+26]<<16)|((uint32_t)cd[pos+27]<<24);
            uint16_t nameLen    = (uint16_t)cd[pos+28]|((uint16_t)cd[pos+29]<<8);
            uint16_t extraLen   = (uint16_t)cd[pos+30]|((uint16_t)cd[pos+31]<<8);
            uint16_t commentLen = (uint16_t)cd[pos+32]|((uint16_t)cd[pos+33]<<8);

            // Hash tên entry (để phân biệt các file bên trong DOCX)
            size_t nameEnd = pos + 46 + nameLen;
            if (nameEnd <= (size_t)cdSize)
                for (size_t i = pos+46; i < nameEnd; i++) fnv(cd[i]);

            // Hash CRC-32 (checksum nội dung — nguồn hash ổn định)
            fnv((unsigned char)(crc32      &0xFF)); fnv((unsigned char)((crc32>>8) &0xFF));
            fnv((unsigned char)((crc32>>16)&0xFF)); fnv((unsigned char)((crc32>>24)&0xFF));
            // Hash uncompressed size (kích thước nội dung thực)
            fnv((unsigned char)(uncompSz      &0xFF)); fnv((unsigned char)((uncompSz>>8) &0xFF));
            fnv((unsigned char)((uncompSz>>16)&0xFF)); fnv((unsigned char)((uncompSz>>24)&0xFF));

            pos += 46 + nameLen + extraLen + commentLen;
        }
        return (h == 0) ? 1 : h;
    }

    // hashFileAsync: tự động chọn chiến lược hash
    void hashFileAsync(std::string path) {
        isProcessing = true; asyncProgress = 0.0f; asyncHashResult = 0;
        std::ifstream chk(path, std::ios::binary | std::ios::ate);
        if (!chk.is_open()) { asyncStatus = -2; isProcessing = false; return; }
        std::streamsize sz = chk.tellg();
        if (sz == 0) { asyncStatus = -1; isProcessing = false; return; }
        chk.close();

        uint64 h = 0;
        if (isOOXMLFile(path)) {
            // DOCX/XLSX/PPTX: chỉ hash CRC-32 từ Central Directory
            h = hashDocxStable(path);
            asyncProgress = 1.0f;
            if (h == 0) {
                // Fallback nếu parse thất bại
                std::ifstream f2(path, std::ios::binary | std::ios::ate);
                std::streamsize sz2 = f2.tellg(); f2.seekg(0);
                h = fnv1a64Stream(f2, sz2, asyncProgress);
            }
        } else {
            // File thông thường: FNV-1a toàn bộ byte
            std::ifstream f2(path, std::ios::binary | std::ios::ate);
            std::streamsize sz2 = f2.tellg(); f2.seekg(0);
            h = fnv1a64Stream(f2, sz2, asyncProgress);
        }

        asyncHashResult = (h == 0) ? 1 : h;
        asyncStatus = 1;
        isProcessing = false;
    }
    
    uint64 hashString(const std::string& s) {
        uint64 h = 14695981039346656037ULL;
        for (char c : s) {
            h ^= (unsigned char)c;
            h *= 1099511628211ULL;
        }
        return h == 0 ? 1 : h;
    }
    
    bool exportTextAndSignature(const std::string& text, const Signature& sig,
                                const std::string& base = "vanban_ky") {
        std::ofstream tf(base + ".txt"); if (!tf.is_open()) return false;
        tf << text; tf.close();
        std::ofstream sf(base + ".txt.sig"); if (!sf.is_open()) return false;
        sf << sig.r.val << "\n" << sig.s.val; sf.close(); return true;
    }
    bool exportKeys(const DSAParams& p, const PrivKey& pr, const PubKey& pb,
                    std::string& outPr, std::string& outPb) {
        std::string tag = GetTimeTag();
        outPr = "private_" + tag + ".key"; outPb = "public_" + tag + ".key";
        std::ofstream prf(outPr); if (!prf.is_open()) return false;
        prf << p.p.val << "\n" << p.q.val << "\n" << p.g.val << "\n" << pr.x.val; prf.close();
        std::ofstream pbf(outPb); if (!pbf.is_open()) return false;
        pbf << p.p.val << "\n" << p.q.val << "\n" << p.g.val << "\n" << pb.y.val; pbf.close();
        return true;
    }
    bool backupKeys(const DSAParams& p, const PrivKey& pr, const PubKey& pb, std::string& out) {
        out = "backup_" + GetTimeTag() + ".bak";
        std::ofstream f(out); if (!f.is_open()) return false;
        f << "BACKUP\n" << p.p.val << "\n" << p.q.val << "\n" << p.g.val
          << "\n" << pr.x.val << "\n" << pb.y.val; f.close(); return true;
    }
    bool restoreBackup(const std::string& path, DSAParams& p, PrivKey& pr, PubKey& pb) {
        std::ifstream f(path); if (!f.is_open()) return false;
        std::string tag; f >> tag; if (tag != "BACKUP") return false;
        std::string v[5]; for (int i = 0; i < 5; i++) if (!(f >> v[i])) return false;
        try {
            p.p.val  = uint_large(v[0]);
            p.q.val  = uint_large(v[1]);
            p.g.val  = uint_large(v[2]);
            pr.x.val = uint_large(v[3]);
            pb.y.val = uint_large(v[4]);
            return true;
        } catch (...) { return false; }
    }
    bool exportSignature(const std::string& origFile, const Signature& sig) {
        std::ofstream sf(origFile + ".sig"); if (!sf.is_open()) return false;
        sf << sig.r.val << "\n" << sig.s.val; sf.close(); return true;
    }
    bool loadPublicKey(const std::string& path, DSAParams& p, PubKey& pb) {
        std::ifstream f(path); if (!f.is_open()) return false;
        std::string pv, qv, gv, yv; if (!(f >> pv >> qv >> gv >> yv)) return false;
        try {
            p.p.val  = uint_large(pv);
            p.q.val  = uint_large(qv);
            p.g.val  = uint_large(gv);
            pb.y.val = uint_large(yv);
            return true;
        } catch (...) { return false; }
    }
    bool loadSignature(const std::string& path, Signature& sig) {
        std::ifstream f(path); if (!f.is_open()) return false;
        std::string rv, sv; if (!(f >> rv >> sv)) return false;
        try {
            sig.r.val = uint_large(rv);
            sig.s.val = uint_large(sv);
            return true;
        } catch (...) { return false; }
    }
    bool exportLog(const std::vector<LogEntry>& logs) {
        std::string fn = "nhat_ky_" + GetTimeTag() + ".txt";
        std::ofstream f(fn); if (!f.is_open()) return false;
        f << "=== LỊCH SỬ HOẠT ĐỘNG CHỮ KÝ SỐ DSA ===\n";
        for (auto& e : logs)
            f << "[" << e.timestamp << "] [" << e.action << "] "
              << (e.success ? "THÀNH CÔNG" : "THẤT BẠI") << " - " << e.detail << "\n";
        f.close(); return true;
    }
}

// ========================================================================
// 5. TOÁN HỌC MẬT MÃ DSA 
// ========================================================================
namespace MathDSA {
    boost::random::mt11213b baseGen(time(nullptr));

    uint_large generateLargeRandom(unsigned bits) {
        boost::random::uniform_int_distribution<uint_large> dist(
            (uint_large(1) << (bits - 1)), (uint_large(1) << bits) - 1
        );
        return dist(baseGen);
    }

    uint_large powerMod(uint_large base, uint_large exp, uint_large mod) {
        return powm(base, exp, mod); 
    }

    bool isPrime(uint_large n, int k = 10) {
        if (n <= 1) return false;
        if (n == 2 || n == 3) return true;
        if (n % 2 == 0) return false;
        return miller_rabin_test(n, k, baseGen); 
    }

    uint_large modInverse(uint_large a, uint_large m) {
        uint_large m0 = m, y = 0, x = 1, al = a;
        if (m == 1) return 0;
        while (al > 1) {
            uint_large q = al / m0, t = m0;
            m0 = al % m0; al = t;
            t = y; y = x - q * y; x = t;
        }
        if (x < 0) x += m;
        return x;
    }
}

namespace DSA {
    DSAParams generateParams(int paramBitMode = 0) {
        DSAParams p_sys; uint_large q, pr, k, h, g;
        unsigned pBits = 512, qBits = 160; 

        if (paramBitMode == 2)      { pBits = 2048; qBits = 256; } 
        else if (paramBitMode == 1) { pBits = 1024; qBits = 160; } 

        while (true) { 
            q = MathDSA::generateLargeRandom(qBits); 
            if (MathDSA::isPrime(q)) break; 
        }
        while (true) { 
            k = MathDSA::generateLargeRandom(pBits - qBits); 
            pr = k * q + 1; 
            if (bit_test(pr, pBits - 1) && MathDSA::isPrime(pr)) break; 
        }
        h = 2;
        while (true) { 
            g = MathDSA::powerMod(h, (pr - 1) / q, pr); 
            if (g > 1) break; 
            h++; 
        }
        p_sys.p = pr; p_sys.q = q; p_sys.g = g; return p_sys;
    }

    // Luồng Worker xử lý Background cho Generate Params
    void generateParamsAsync(int mode) {
        isProcessing = true;
        asyncOperation = 3;
        gTempParams = generateParams(mode);
        asyncStatus = 3;
        isProcessing = false;
    }

    std::pair<PrivKey, PubKey> generateKeyPair(const DSAParams& p) {
        PrivKey priv; PubKey pub;
        priv.x = MathDSA::generateLargeRandom(256) % (p.q.val - 1) + 1;
        pub.y  = MathDSA::powerMod(p.g.val, priv.x.val, p.p.val);
        return {priv, pub};
    }

    Signature signHash(uint64 hm_64, const PrivKey& key, const DSAParams& p) {
        uint_large hm(hm_64); 
        uint_large k, r, s;
        do {
            do { 
                k = MathDSA::generateLargeRandom(256) % (p.q.val - 1) + 1; 
                r = MathDSA::powerMod(p.g.val, k, p.p.val) % p.q.val; 
            } while (r == 0);
            uint_large ki = MathDSA::modInverse(k, p.q.val);
            uint_large xr = (key.x.val * r) % p.q.val;
            s = (ki * ((hm + xr) % p.q.val)) % p.q.val;
        } while (s == 0);
        Signature sig; sig.r = r; sig.s = s; return sig;
    }

    bool verifyHash(uint64 hm_64, const Signature& sig, const PubKey& key, const DSAParams& p) {
        uint_large r = sig.r.val, s = sig.s.val, hm(hm_64);
        if (r == 0 || r >= p.q.val || s == 0 || s >= p.q.val) return false;
        uint_large w   = MathDSA::modInverse(s, p.q.val);
        uint_large u1  = (hm * w) % p.q.val;
        uint_large u2  = (r * w) % p.q.val;
        uint_large gu1 = MathDSA::powerMod(p.g.val, u1, p.p.val);
        uint_large yu2 = MathDSA::powerMod(key.y.val, u2, p.p.val);
        uint_large v   = ((gu1 * yu2) % p.p.val) % p.q.val;
        return v == r;
    }

    int diagnoseFailure(uint64 hashCurrent,
                        const Signature& sigLoaded,   
                        const PubKey&    keyProvided, 
                        const DSAParams& p,
                        uint64           hashOriginal,
                        const Signature& sigOriginal,
                        const PubKey&    keyOriginal,
                        bool hasOriginalInfo) {

        if (!hasOriginalInfo) {
            // Không có thông tin phiên ký gốc, phân tích dựa trên cấu trúc toán học
            uint_large r = sigLoaded.r.val, s = sigLoaded.s.val;
            // Kiểm tra r, s có hợp lệ về mặt phạm vi không
            if (r == 0 || r >= p.q.val || s == 0 || s >= p.q.val) {
                // r hoặc s nằm ngoài phạm vi hợp lệ -> chữ ký bị sửa cấu trúc
                return -5;
            }
            // Không có gì để so sánh -> chỉ biết xác minh thất bại chung
            return -1; 
        }

        bool dataTampered = (hashCurrent != hashOriginal);
        bool sigTampered  = (sigLoaded.r.val != sigOriginal.r.val || sigLoaded.s.val != sigOriginal.s.val);
        bool keyTampered  = (keyProvided.y.val != keyOriginal.y.val);

        if (dataTampered && sigTampered && keyTampered)    return -4; // Cả 3 bị sửa
        if (!dataTampered && sigTampered && keyTampered)   return -7; // Chỉ sig + key bị sửa
        if (dataTampered && sigTampered && !keyTampered)   return -6; // Data + sig bị sửa
        if (dataTampered && !sigTampered && keyTampered)   return -8; // Data + key bị sửa
        if (!dataTampered && !sigTampered && keyTampered)  return -3; // Chỉ key bị sửa
        if (!dataTampered && sigTampered && !keyTampered)  return -5; // Chỉ sig bị sửa
        if (dataTampered && !sigTampered && !keyTampered)  return -1; // Chỉ data bị sửa

        return -1;
    }
} 

// ========================================================================
// 6. CÁC HÀM VẼ GIAO DIỆN
// ========================================================================
void TXT(const char* t, int x, int y, int sz, Color c) {
    DrawTextEx(appFont, t, {(float)x,(float)y}, (float)sz, 1.0f, c);
}

void DrawLabelBox(int x, int y, int w, int boxH,
                  const char* label, const std::string& content,
                  Color contentColor, int labelSz = 15, int contentSz = 16) {
    TXT(label, x, y, labelSz, {80, 110, 160, 255});
    int boxY = y + labelSz + 5;
    // Shadow
    DrawRectangle(x+2, boxY+3, w, boxH, {0,0,0,18});
    // Nền card
    DrawRectangle(x, boxY, w, boxH, CARD_BG);
    // Left accent bar theo màu content
    DrawRectangle(x, boxY, 4, boxH, ColorAlpha(contentColor, 0.7f));
    // Border
    DrawRectangleLinesEx({(float)x,(float)boxY,(float)w,(float)boxH}, 1, BORDER_COLOR);
    // Top highlight
    DrawLine(x, boxY, x+w, boxY, {255,255,255,180});

    int posX = x + 16;
    int lineW = w - 32;
    std::vector<std::string> lines;
    std::string cur;
    for (size_t i = 0; i < content.size(); i++) {
        cur += content[i];
        Vector2 ts = MeasureTextEx(appFont, cur.c_str(), contentSz, 1.0f);
        if (ts.x >= lineW || content[i] == '\n') {
            lines.push_back(cur); cur = "";
        }
    }
    if (!cur.empty()) lines.push_back(cur);
    int totalH = (int)lines.size() * (contentSz + 4);
    int posY = boxY + (boxH - totalH) / 2;
    if (posY < boxY + 6) posY = boxY + 6;
    for (auto& l : lines) {
        TXT(l.c_str(), posX, posY, contentSz, contentColor);
        posY += contentSz + 4;
    }
}
inline int LBH(int labelSz, int boxH) { return labelSz + 6 + boxH; }

bool Btn(int x, int y, int w, int h, const char* txt, bool sel = false, Color base = BTN_TEAL) {
    bool hov = CheckCollisionPointRec(gVirtualMouse, {(float)x,(float)y,(float)w,(float)h});
    bool clk = hov && IsMouseButtonPressed(MOUSE_BUTTON_LEFT);
    bool pressed = hov && IsMouseButtonDown(MOUSE_BUTTON_LEFT);

    Color bg;
    if (sel) {
        bg = base;
    } else if (pressed) {
        bg = Color{(unsigned char)std::max(0,(int)base.r-20),
                   (unsigned char)std::max(0,(int)base.g-20),
                   (unsigned char)std::max(0,(int)base.b-20), 255};
    } else if (hov) {
        bg = Color{(unsigned char)std::min(255,(int)base.r+18),
                   (unsigned char)std::min(255,(int)base.g+18),
                   (unsigned char)std::min(255,(int)base.b+18), 255};
    } else {
        bg = Color{228,234,242,255};
    }

    // Shadow (chỉ khi không pressed)
    if (!pressed)
        DrawRectangle(x+1, y+2, w, h, {0,0,0,22});

    // Nền
    DrawRectangle(x, y, w, h, bg);

    // Top highlight
    DrawLine(x+1, y+1, x+w-1, y+1, {255,255,255,(unsigned char)(sel?60:80)});

    // Border
    DrawRectangleLinesEx({(float)x,(float)y,(float)w,(float)h}, 1,
        sel ? ColorAlpha(base,0.6f) : ColorAlpha(base,0.3f));

    Vector2 ts = MeasureTextEx(appFont, txt, 15, 1.0f);
    Color tc = (sel||hov) ? WHITE : Color{50,65,90,255};
    // Text shadow nhẹ khi active
    if (sel||hov) TXT(txt, x+(w-(int)ts.x)/2+1, y+(h-15)/2+1, 15, {0,0,0,40});
    TXT(txt, x+(w-(int)ts.x)/2, y+(h-15)/2, 15, tc);
    return clk;
}

void DropZone(int x, int y, int w, int h, const char* label, const std::string& fp) {
    TXT(label, x, y, 14, {75, 105, 155, 255});
    int by = y + 14 + 5;
    int dropW = fp.empty() ? w : w - 50;

    if (fp.empty()) {
        // Nền xám nhạt
        DrawRectangle(x, by, dropW, h, {248,251,255,255});
        // Dashed border giả lập bằng các đoạn thẳng
        Color dbc = {160,180,210,255};
        int dash = 8, gap = 6;
        // Top & bottom
        for (int dx = x; dx < x+dropW; dx += dash+gap) {
            int end = std::min(dx+dash, x+dropW);
            DrawLine(dx, by, end, by, dbc);
            DrawLine(dx, by+h, end, by+h, dbc);
        }
        // Left & right
        for (int dy = by; dy < by+h; dy += dash+gap) {
            int end = std::min(dy+dash, by+h);
            DrawLine(x, dy, x, end, dbc);
            DrawLine(x+dropW, dy, x+dropW, end, dbc);
        }
        const char* hint = "KÉO VÀ THẢ FILE VÀO ĐÂY...";
        Vector2 ts = MeasureTextEx(appFont, hint, 14, 1.0f);
        TXT(hint, x + (dropW-(int)ts.x)/2, by + (h-14)/2, 14, {160,180,210,255});
    } else {
        // Đã có file — nền xanh nhạt
        DrawRectangle(x, by, dropW, h, {235,252,242,255});
        DrawRectangleLinesEx({(float)x,(float)by,(float)dropW,(float)h}, 2, SUCCESS_COLOR);
        // Checkmark nhỏ
        DrawCircle(x+20, by+h/2, 8, SUCCESS_COLOR);
        TXT("v", x+17, by+h/2-7, 14, WHITE);
        std::string dp = fp.length() > 65 ? "..." + fp.substr(fp.length()-62) : fp;
        TXT(("Đã tải: " + dp).c_str(), x+36, by + (h-14)/2, 14, {15,120,70,255});
    }
}

// Nút xoá file bên cạnh DropZone
bool DropZoneClearBtn(int x, int y, int h, const std::string& fp) {
    if (fp.empty()) return false;
    int by = y + 14 + 5;
    int bx = x - 46;
    bool hov = CheckCollisionPointRec(gVirtualMouse, {(float)bx,(float)by,42.0f,(float)h});
    bool clk = hov && IsMouseButtonPressed(MOUSE_BUTTON_LEFT);
    bool pressed = hov && IsMouseButtonDown(MOUSE_BUTTON_LEFT);
    // Shadow
    if (!pressed) DrawRectangle(bx+1, by+2, 42, h, {0,0,0,25});
    Color btnC = pressed ? Color{180,20,20,255} :
                 hov     ? Color{235,50,50,255} :
                           Color{215,40,40,255};
    DrawRectangle(bx, by, 42, h, btnC);
    DrawLine(bx+1, by+1, bx+41, by+1, {255,255,255,60});
    DrawRectangleLinesEx({(float)bx,(float)by,42.0f,(float)h}, 1, {180,20,20,255});
    // X icon
    int cx2 = bx+21, cy2 = by+h/2;
    int arm = 6;
    DrawLine(cx2-arm,cy2-arm, cx2+arm,cy2+arm, WHITE);
    DrawLine(cx2+arm,cy2-arm, cx2-arm,cy2+arm, WHITE);
    return clk;
}
inline int DZH(int h) { return 15 + 6 + h; } 

void Badge(int x, int y, const char* txt, Color bg, int sz = 14) {
    Vector2 ts = MeasureTextEx(appFont, txt, sz, 1.0f);
    int pw = (int)ts.x + 18, ph = sz + 10;
    // Shadow
    DrawRectangle(x+1, y+2, pw, ph, {0,0,0,20});
    // Nền
    DrawRectangle(x, y, pw, ph, bg);
    // Top highlight
    DrawLine(x+1, y+1, x+pw-1, y+1, {255,255,255,70});
    // Border nhạt
    DrawRectangleLinesEx({(float)x,(float)y,(float)pw,(float)ph}, 1,
        ColorAlpha(bg, 0.5f));
    TXT(txt, x+9, y+5, sz, WHITE);
}

void SecHeader(const char* title) {
    // Gradient header
    DrawRectangleGradientH(SW, 0, VW-SW, 52, HEADER_BLUE, {22,52,120,255});
    // Bottom glow
    DrawRectangleGradientV(SW, 50, VW-SW, 4, {59,130,246,120}, {59,130,246,0});
    // Title shadow
    TXT(title, CX+1, 15, 19, {0,0,0,60});
    TXT(title, CX, 14, 19, WHITE);
}

void HLine(int y) { DrawLine(SW, y, VW, y, BORDER_COLOR); }

// Load Font tiếng Việt xịn
void LoadVietnameseFont() {
    int cps[1600] = {0}; int n = 0;
    for (int i = 32;     i < 127;   i++) cps[n++] = i;
    for (int i = 0x00A0; i < 0x0259;i++) cps[n++] = i;
    for (int i = 0x1EA0; i < 0x1EFE;i++) cps[n++] = i;
    appFont = LoadFontEx("ARIAL.TTF", 48, cps, n);
    SetTextureFilter(appFont.texture, TEXTURE_FILTER_BILINEAR);
}

// ========================================================================
// 7. MAIN CONTROLLER
// ========================================================================
int main() {
    srand((unsigned)time(NULL));
    SetConfigFlags(FLAG_WINDOW_RESIZABLE);
    InitWindow(VW, VH, "Mô phỏng Hệ chữ ký số DSA - Nhóm 10");
    SetWindowMinSize(960, 700);
    SetTargetFPS(60);
    LoadVietnameseFont();
    // Load logo, chuyển nền trắng thành trong suốt, chữ HaUI xanh đậm thành trắng
    {
        Image logoImg = LoadImage("haui_logo.png");
        if (logoImg.data != NULL) {
            ImageFormat(&logoImg, PIXELFORMAT_UNCOMPRESSED_R8G8B8A8);
            Color* pixels = (Color*)logoImg.data;
            int totalPixels = logoImg.width * logoImg.height;
            for (int pi = 0; pi < totalPixels; pi++) {
                Color& c = pixels[pi];
                if (c.r > 220 && c.g > 220 && c.b > 220) {
                    // Pixel trắng/sáng → trong suốt hoàn toàn
                    c.a = 0;
                } else if (c.r > 180 && c.g > 180 && c.b > 180) {
                    // Pixel xám nhạt → bán trong suốt (viền mượt)
                    int bright = (c.r + c.g + c.b) / 3;
                    c.a = (unsigned char)(255 - (bright - 180) * 255 / 75);
                } else {
                    // Pixel màu còn lại (chữ HaUI xanh đậm, biểu tượng...):
                    // Nếu là màu xanh đậm đặc trưng của chữ HaUI
                    // (B là kênh lớn nhất, R và G thấp) → đổi thành trắng
                    if (c.b > c.r * 2 && c.b > c.g * 2 && c.b > 80) {
                        c.r = 255; c.g = 255; c.b = 255;
                    }
                }
            }
            logoHaUI = LoadTextureFromImage(logoImg);
            UnloadImage(logoImg);
        }
    }

    Tab currentTab = HOME;
    std::string statusMsg = "Hệ thống sẵn sàng. Vui lòng làm theo quy trình ở Trang chủ.";
    Color statusCol = SUCCESS_COLOR;

    bool confirmRevoke = false; int confirmRevokeIdx = -1;
    std::string backupFileName, savedPrivName, savedPubName;
    bool showBackupDone = false;
    int  verifyMode = 0; 
    int  paramBitMode = 0; 
    int  tempParamMode = 0; 

    while (!WindowShouldClose()) {
        float dt = GetFrameTime();
        gTime += dt;
        // Cập nhật pulse cho nút CTA
        gBtnPulse = 0.5f + 0.5f * sinf(gTime * 2.2f);
        // Tab content fade khi đổi tab
        if (currentTab != gPrevTab) {
            gContentAlpha = 0.0f;
            gPrevTab = currentTab;
        }
        gContentAlpha = std::min(1.0f, gContentAlpha + dt * 6.0f);
        // Status fade khi đổi message
        // (dùng trong status bar drawing)

        if (!isProcessing) {
            if (currentTab == KEY_SIGN) {
                int k = GetCharPressed();
                while (k > 0) {
                    if (k >= 32 && k <= 125 && gState.textInputLen < 510) {
                        gState.textInput[gState.textInputLen++] = (char)k;
                        gState.textInput[gState.textInputLen] = '\0';
                    }
                    k = GetCharPressed();
                }
                if (IsKeyPressed(KEY_BACKSPACE) && gState.textInputLen > 0)
                    gState.textInput[--gState.textInputLen] = '\0';
            }
            if (currentTab == VERIFY && verifyMode == 1) {
                int k = GetCharPressed();
                while (k > 0) {
                    if (k >= 32 && k <= 125 && gState.verifyTextInputLen < 510) {
                        gState.verifyTextInput[gState.verifyTextInputLen++] = (char)k;
                        gState.verifyTextInput[gState.verifyTextInputLen] = '\0';
                    }
                    k = GetCharPressed();
                }
                if (IsKeyPressed(KEY_BACKSPACE) && gState.verifyTextInputLen > 0)
                    gState.verifyTextInput[--gState.verifyTextInputLen] = '\0';
            }
        }

        float scaleX  = (float)GetScreenWidth()  / (float)VW;
        float scaleY  = (float)GetScreenHeight() / (float)VH;
        Vector2 rawMouse = GetMousePosition();
        gVirtualMouse = { rawMouse.x / scaleX, rawMouse.y / scaleY };

        if (IsFileDropped()) {
            FilePathList fl = LoadDroppedFiles();
            std::string path = fl.paths[0];
            if (currentTab == KEY_SIGN && !isProcessing) {
                if (path.find(".bak") != std::string::npos) {
                    DSAParams rp; PrivKey rpv; PubKey rpb;
                    if (FileIOHelper::restoreBackup(path, rp, rpv, rpb)) {
                        gState.params = rp; gState.privKey = rpv; gState.pubKey = rpb;
                        gState.hasParams = gState.hasKeys = true;
                        statusMsg = "Đã khôi phục từ tệp sao lưu: " + path; statusCol = SUCCESS_COLOR;
                        AddLog("KHÔI PHỤC", path, true);
                    } else { statusMsg = "LỖI: Tệp sao lưu (.bak) không hợp lệ!"; statusCol = DANGER_COLOR; }
                } else { gState.fileToSignPath = path; statusMsg = "Đã tải file. Bấm 'KÝ FILE' để tiếp tục!"; statusCol = HEADER_BLUE; }
            }
            else if (currentTab == VERIFY && !isProcessing) {
                if      (path.find(".sig") != std::string::npos) gState.verifySigPath    = path;
                else if (path.find(".key") != std::string::npos) gState.verifyPubKeyPath = path;
                else                                              gState.verifyFilePath   = path;
                statusMsg = "Đã nhận diện và tải file vào vùng tương ứng."; statusCol = HEADER_BLUE;
            }
            else if (currentTab == PARAM_GEN && !isProcessing) {
                if (path.find(".bak") != std::string::npos) {
                    DSAParams rp; PrivKey rpv; PubKey rpb;
                    if (FileIOHelper::restoreBackup(path, rp, rpv, rpb)) {
                        gState.params = rp; gState.privKey = rpv; gState.pubKey = rpb;
                        gState.hasParams = gState.hasKeys = true;
                        statusMsg = "Đã khôi phục toàn bộ tham số từ tệp sao lưu!"; statusCol = SUCCESS_COLOR;
                        AddLog("KHÔI PHỤC", "Restore: " + path, true);
                    } else { statusMsg = "LỖI: Tệp sao lưu không hợp lệ!"; statusCol = DANGER_COLOR; }
                } else if (path.find(".key") != std::string::npos) {
                    DSAParams lp; PubKey lpb;
                    if (FileIOHelper::loadPublicKey(path, lp, lpb)) {
                        gState.params = lp; gState.pubKey = lpb;
                        gState.hasParams = gState.hasKeys = true;
                        statusMsg = "Đã tải Khóa công khai từ: " + path; statusCol = SUCCESS_COLOR;
                        AddLog("TẢI KHÓA", path, true);
                        std::string fn = path.length() > 60 ? path.substr(path.length()-60) : path;
                        gState.savedKeyFiles.push_back(fn); gState.revokedKeys.push_back(false);
                    } else { statusMsg = "LỖI: Tệp khóa (.key) bị hỏng hoặc sai cấu trúc!"; statusCol = DANGER_COLOR; }
                }
            }
            UnloadDroppedFiles(fl);
        }

        float wh = GetMouseWheelMove();
        if (wh != 0) {
            int delta = (int)(wh * 30);
            if (currentTab == TAB_LOG)   gState.logScrollY = std::max(0, gState.logScrollY - delta);
            if (currentTab == KEY_SIGN)  gState.docScrollY = std::max(0, gState.docScrollY - delta);
            if (currentTab == PARAM_GEN) gState.keyScrollY = std::max(0, gState.keyScrollY - delta);
        }

        // ============================================================
        // VẼ GIAO DIỆN
        // ============================================================
        BeginDrawing();
        ClearBackground(BG_COLOR);
        rlPushMatrix();
        rlScalef(scaleX, scaleY, 1.0f);
        // Nền với gradient nhẹ từ trên xuống
        DrawRectangleGradientV(SW, 0, VW-SW, VH, {235,241,252,255}, {228,236,248,255});

        // ============================================================
        // SIDEBAR — dark gradient, animated tab highlights
        // ============================================================
        // Nền gradient tối
        DrawRectangleGradientV(0, 0, SW, VH, SB_BG1, SB_BG2);
        // Viền phải mờ
        DrawRectangleGradientH(SW-3, 0, 3, VH, {0,0,0,0}, {0,0,0,40});

        // Header khu vực
        DrawRectangleGradientH(0, 0, SW, 60, {8,20,60,255}, {16,38,90,255});
        DrawLine(0, 60, SW, 60, {59,130,246,60});
        // Logo text
        TXT("MO PHONG", 14, 8, 13, {140,180,240,200});
        TXT("CHU KY SO DSA", 14, 26, 16, WHITE);
        // Accent dot
        DrawCircle(SW-18, 30, 5, SB_ACCENT);
        DrawCircle(SW-18, 30, 3, WHITE);

        const char* tabLabels[] = {
            "Trang chu",
            "1. Tham so He thong",
            "2. Quan ly Khoa & Ky",
            "3. Xac minh Chu ky",
            "4. Ly thuyet Co che",
            "5. Nhat ky Hoat dong"
        };
        Tab tabVals[] = {HOME, PARAM_GEN, KEY_SIGN, VERIFY, THEORY, TAB_LOG};

        for (int i = 0; i < 6; i++) {
            int ty2 = 68 + i * 52;
            bool sel = (currentTab == tabVals[i]);
            bool hov = CheckCollisionPointRec(gVirtualMouse,
                {0,(float)ty2,(float)SW,50.0f});
            if (!isProcessing && hov && IsMouseButtonPressed(MOUSE_BUTTON_LEFT))
                currentTab = tabVals[i];

            // Animate hover
            gTabAnim[i] = sel ? 1.0f :
                          (hov ? std::min(1.0f, gTabAnim[i] + GetFrameTime()*8)
                               : std::max(0.0f, gTabAnim[i] - GetFrameTime()*6));

            float a = gTabAnim[i];
            if (a > 0.01f) {
                // Background highlight
                Color hlBg = sel ? Color{59,130,246,200} : Color{255,255,255,(unsigned char)(40*a)};
                DrawRectangle(0, ty2, SW, 50, hlBg);
                // Left accent bar
                int barH = (int)(50 * a);
                int barY = ty2 + (50 - barH) / 2;
                DrawRectangle(0, barY, 4, barH,
                    Color{(unsigned char)(100+155*a),(unsigned char)(180+75*a),255,255});
            }
            // Divider line
            if (i > 0)
                DrawLine(12, ty2, SW-12, ty2, {255,255,255,15});

            // Icon số tròn
            int iconX = 16, iconY = ty2 + 16;
            Color iconBg = sel ? SB_ACCENT :
                Color{255,255,255,(unsigned char)(int)(30 + 30*a)};
            DrawCircle(iconX+9, iconY+9, 10, iconBg);
            char nb2[3]; snprintf(nb2, 3, "%d", i);
            Vector2 nts2 = MeasureTextEx(appFont, nb2, 12, 1.0f);
            TXT(nb2, iconX+9-(int)nts2.x/2, iconY+3, 12, WHITE);

            // Label
            Color tc2 = sel ? WHITE :
                Color{(unsigned char)(140+int(70*a)),
                      (unsigned char)(175+int(35*a)),
                      (unsigned char)(220+int(35*a)), 255};
            TXT(tabLabels[i], 44, ty2+17, 14, tc2);
        }

        // Status badges
        int by = 68 + 6*52 + 14;
        DrawLine(10, by, SW-10, by, {255,255,255,25}); by += 12;
        TXT("Trang thai:", 12, by, 13, {140,180,240,180}); by += 22;

        auto SBadge = [&](const char* t, bool active, Color ac) {
            Vector2 ts2 = MeasureTextEx(appFont, t, 13, 1.0f);
            int bw = (int)ts2.x + 20;
            Color bg2 = active ? ac : Color{255,255,255,20};
            DrawRectangle(12, by, bw, 24, bg2);
            if (active) DrawLine(12, by, 12+bw, by, {255,255,255,50});
            TXT(t, 21, by+5, 13, active ? WHITE : Color{160,195,240,200});
            by += 30;
        };
        SBadge(gState.hasParams ? "DA CO THAM SO" : "CHUA CO THAM SO",
               gState.hasParams, SUCCESS_COLOR);
        SBadge(gState.hasKeys ? "DA TAO KHOA" : "CHUA TAO KHOA",
               gState.hasKeys, SUCCESS_COLOR);
        SBadge(gState.hasSignature ? "DA KY FILE" : "CHUA KY FILE",
               gState.hasSignature, WARNING_COLOR);

        // Logo + info — căn giữa, nền trắng đã được xử lý thành trong suốt khi load
        if (logoHaUI.id != 0) {
            float sc  = 180.0f / logoHaUI.width;
            int imgW  = (int)(logoHaUI.width  * sc);
            int imgH  = (int)(logoHaUI.height * sc);
            int iy    = VH - imgH - 58;
            int ix    = (SW - imgW) / 2; // căn giữa theo chiều ngang sidebar

            // Dùng BLEND_ALPHA bình thường: nền trắng đã trong suốt,
            // logo hiển thị đúng màu sắc sáng trực tiếp lên nền sidebar tối
            DrawTextureEx(logoHaUI, {(float)ix, (float)iy}, 0.0f, sc, WHITE);
        }

        // Text căn giữa sidebar
        {
            const char* t1 = "Nhom 10 - Lop IT6001.2";
            const char* t2 = "GVHD: TS. Pham Van Hiep";
            Vector2 s1 = MeasureTextEx(appFont, t1, 13, 1.0f);
            Vector2 s2 = MeasureTextEx(appFont, t2, 13, 1.0f);
            int x1 = (SW - (int)s1.x) / 2;
            int x2 = (SW - (int)s2.x) / 2;
            DrawLine(10, VH-54, SW-10, VH-54, {255,255,255,25});
            TXT(t1, x1, VH-48, 13, {140,180,240,180});
            TXT(t2, x2, VH-30, 13, {180,210,255,220});
        }

        // ============================================================
        // THANH TRẠNG THÁI — animated glow
        // ============================================================
        // Nền trắng với màu tint theo loại thông báo
        DrawRectangle(SW, STATUS_Y, VW-SW, VH-STATUS_Y, WHITE);
        // Dải màu top animate
        float pulseA = 0.12f + 0.06f * sinf(gTime * 3.0f);
        DrawRectangleGradientH(SW, STATUS_Y, VW-SW, 3,
            ColorAlpha(statusCol, 0.9f), ColorAlpha(statusCol, 0.3f));
        // Tint nền nhẹ
        DrawRectangle(SW, STATUS_Y+3, VW-SW, VH-STATUS_Y-3, ColorAlpha(statusCol, pulseA));
        // Indicator dot (animate nhẹ)
        float dotR = 6.0f + 1.5f * sinf(gTime * 3.0f);
        DrawCircle(SW+22, STATUS_Y + (VH-STATUS_Y)/2, dotR+2, ColorAlpha(statusCol,0.3f));
        DrawCircle(SW+22, STATUS_Y + (VH-STATUS_Y)/2, dotR, statusCol);
        TXT("Trang thai:", SW+38, STATUS_Y+18, 16, {100,115,135,255});
        TXT(statusMsg.c_str(), SW+130, STATUS_Y+18, 16, statusCol);

        const int TOP  = 62;

        // Fade-in content khi đổi tab
        if (gContentAlpha < 0.99f) {
            // Overlay tối mờ dần
            DrawRectangle(SW, 0, VW-SW, STATUS_Y,
                {240,244,250,(unsigned char)(int)((1.0f-gContentAlpha)*180)});
        }

        switch (currentTab) {

        // ------------------------------------------------------------
        case HOME: {
            DrawRectangle(SW, 0, VW-SW, 52, HEADER_BLUE);
            TXT("DSA - Thuật toán Chữ ký số", CX, 8, 26, WHITE);
            TXT("Chuẩn bảo mật FIPS 186 | Khóa 2048-bit | Thuật toán băm Stream", CX, 36, 15, {200,220,255,255});

            int y = TOP + 8;
            TXT("HƯỚNG DẪN SỬ DỤNG TRỰC QUAN", CX, y, 18, HEADER_BLUE); y += 30;

            auto Step = [&](int idx, const char* title, const char* desc) {
                bool hov = CheckCollisionPointRec(gVirtualMouse,
                    {(float)CX,(float)y,(float)CW,58.0f});
                // Shadow
                DrawRectangle(CX+2, y+3, CW, 58, {0,0,0,16});
                Color cardBg = hov ? Color{248,252,255,255} : CARD_BG;
                DrawRectangle(CX, y, CW, 58, cardBg);
                // Left accent gradient
                DrawRectangleGradientV(CX, y, 5, 58,
                    {37,99,235,255}, {99,155,245,200});
                DrawRectangleLines(CX, y, CW, 58, hov ? Color{180,210,250,255} : BORDER_COLOR);
                // Top highlight
                DrawLine(CX+1,y+1,CX+CW-1,y+1,{255,255,255,180});
                // Step circle
                DrawCircle(CX+28, y+29, 20, HEADER_BLUE);
                DrawCircle(CX+28, y+28, 20,
                    Color{37,99,235,255});
                char nb[4]; snprintf(nb,4,"%d",idx);
                Vector2 nts = MeasureTextEx(appFont, nb, 18, 1.0f);
                TXT(nb, CX+28-(int)nts.x/2, y+20, 18, WHITE);
                TXT(title, CX+58, y+9,  15, TEXT_HEADING);
                TXT(desc,  CX+58, y+32, 13, TEXT_SUB);
                y += 66;
            };
            Step(1,"Tab 1 - Tham số Hệ thống","Sinh tham số nền tảng (p,q,g). Quản lý danh sách khóa đã thu hồi.");
            Step(2,"Tab 2 - Quản lý Khóa & Ký","Tạo Khóa Bí mật/Công khai. Gõ văn bản hoặc kéo thả File vào để tạo Chữ ký số.");
            Step(3,"Tab 3 - Xác minh Chữ ký","Kéo thả File gốc, File .sig và File .key để thuật toán đối chiếu toàn vẹn.");
            Step(4,"Tab 4 - Lý thuyết Cơ chế","Kiến thức chuyên sâu về bài toán Logarit rời rạc và luồng bảo mật của DSA.");
            Step(5,"Tab 5 - Nhật ký Hệ thống","Giám sát lịch sử toàn bộ các thao tác Ký và Xác minh trong phiên làm việc.");

            y += 8;
            TXT("CÔNG THỨC TOÁN HỌC CỐT LÕI", CX, y, 18, HEADER_BLUE); y += 30;
            int hw = (CW - 12) / 2;
            int boxHeight = 175;
            
            // Box trái — Ký số
            DrawRectangle(CX+2, y+3, hw, boxHeight, {0,0,0,18});
            DrawRectangleGradientV(CX, y, hw, boxHeight, {240,248,255,255},{232,244,255,255});
            DrawRectangleLinesEx({(float)CX,(float)y,(float)hw,(float)boxHeight},1,{190,215,245,255});
            DrawRectangle(CX, y, 4, boxHeight, {37,99,235,200});
            DrawLine(CX+1,y+1,CX+hw-1,y+1,{255,255,255,200});
            TXT("Quy trinh Ky so (Signing)",     CX+14, y+12, 15, TEXT_HEADING);
            DrawLine(CX+14, y+32, CX+hw-14, y+32, BORDER_COLOR);
            TXT("r = (g^k mod p) mod q",         CX+14, y+42, 15, TEXT_MAIN);
            TXT("s = k^-1*(H(m)+x*r) mod q",     CX+14, y+66, 15, TEXT_MAIN);
            TXT("Chu ky xuat ra: (r, s)",         CX+14, y+90, 15, TEXT_MAIN);
            TXT("k: so ngau nhien giu bi mat",    CX+14, y+125, 13, TEXT_SUB);

            int bx2 = CX + hw + 12;
            DrawRectangle(bx2+2, y+3, hw, boxHeight, {0,0,0,18});
            DrawRectangleGradientV(bx2, y, hw, boxHeight, {240,255,248,255},{232,252,242,255});
            DrawRectangleLinesEx({(float)bx2,(float)y,(float)hw,(float)boxHeight},1,{180,235,210,255});
            DrawRectangle(bx2, y, 4, boxHeight, {4,142,100,200});
            DrawLine(bx2+1,y+1,bx2+hw-1,y+1,{255,255,255,200});
            TXT("Quy trinh Xac thuc (Verification)", bx2+14, y+12, 15, TEXT_HEADING);
            DrawLine(bx2+14, y+32, bx2+hw-14, y+32, BORDER_COLOR);
            TXT("w = s^-1 mod q",                    bx2+14, y+42, 15, TEXT_MAIN);
            TXT("u1 = H(m)*w mod q",                 bx2+14, y+66, 15, TEXT_MAIN);
            TXT("u2 = r*w  mod q",                   bx2+14, y+90, 15, TEXT_MAIN);
            TXT("v = (g^u1*y^u2 mod p)%q",           bx2+14, y+114, 15, TEXT_MAIN);
            TXT("Hop le khi: v == r",                 bx2+14, y+148, 16, {4,142,100,255});
            break;
        }

        // ------------------------------------------------------------
        case PARAM_GEN: {
            SecHeader("TAB 1 - THIẾT LẬP THAM SỐ HỆ THỐNG (p, q, g)");

            int bY = TOP - 4;
            
            TXT("Độ dài khóa (Bit length):", CX, bY + 12, 16, TEXT_MAIN);
            
            bool canClick = !isProcessing;
            if (Btn(CX + 210, bY + 6, 100, 30, "512-bit",  paramBitMode == 0, canClick ? BTN_TEAL : LIGHTGRAY) && canClick) paramBitMode = 0;
            if (Btn(CX + 320, bY + 6, 100, 30, "1024-bit", paramBitMode == 1, canClick ? BTN_TEAL : LIGHTGRAY) && canClick) paramBitMode = 1;
            if (Btn(CX + 430, bY + 6, 100, 30, "2048-bit", paramBitMode == 2, canClick ? DANGER_COLOR : LIGHTGRAY) && canClick) paramBitMode = 2;

            if (!isProcessing && asyncStatus == 0) {
                if (Btn(CX, bY + 48, 200, 42, "TẠO THAM SỐ MỚI", false, BTN_TEAL)) {
                    tempParamMode = paramBitMode;
                    std::thread t(DSA::generateParamsAsync, paramBitMode);
                    t.detach();
                }
            } else if (isProcessing && asyncOperation == 3) {
                // UI Chờ khi Thread đang chạy sinh số 2048-bit
                DrawRectangle(CX, bY + 48, 200, 42, Color{220,225,230,255});
                DrawRectangleLines(CX, bY + 48, 200, 42, BORDER_COLOR);
                TXT("ĐANG TÍNH TOÁN...", CX + 30, bY + 60, 16, TEXT_SUB);
                
                int frames = (int)(GetTime() * 8) % 4;
                const char* spin = (frames==0)?"|":(frames==1)?"/":(frames==2)?"-":"\\";
                TXT(TextFormat("Hệ thống đang tìm kiếm số nguyên tố %d-bit siêu lớn, vui lòng đợi %s", 
                     paramBitMode==2?2048:(paramBitMode==1?1024:512), spin), 
                     CX + 220, bY + 60, 16, WARNING_COLOR);
            } else if (!isProcessing && asyncOperation == 3 && asyncStatus == 3) {
                // Thread đã chạy xong
                gState.params = gTempParams;
                gState.hasParams = true; gState.hasKeys = false; gState.hasSignature = false;
                statusMsg = "Khởi tạo thành công! Đã sinh xong các số nguyên tố chuẩn bảo mật.";
                statusCol = SUCCESS_COLOR;
                AddLog("TẠO THAM SỐ", "Thành công (Chế độ " + std::to_string(tempParamMode) + ")", true);
                asyncStatus = 0; asyncOperation = 0;
            }

            if (gState.hasKeys && !isProcessing) {
                if (Btn(CX+210, bY+48, 195, 42, "LƯU KHÓA (.KEY)", false, WARNING_COLOR)) {
                    std::string pn, bn;
                    if (FileIOHelper::exportKeys(gState.params,gState.privKey,gState.pubKey,pn,bn)) {
                        savedPrivName=pn; savedPubName=bn;
                        statusMsg="Đã xuất khóa ra thư mục: " + pn + " & " + bn; statusCol=SUCCESS_COLOR;
                        AddLog("LƯU KHÓA", bn, true);
                        gState.savedKeyFiles.push_back(bn); gState.revokedKeys.push_back(false);
                    } else { statusMsg="LỖI: Không thể ghi file ra ổ cứng!"; statusCol=DANGER_COLOR; AddLog("LƯU KHÓA","Thất bại",false); }
                }
                if (Btn(CX+415, bY+48, 200, 42, "SAO LƯU (.BAK)", false, {90,150,215,255})) {
                    std::string bname;
                    if (FileIOHelper::backupKeys(gState.params,gState.privKey,gState.pubKey,bname)) {
                        backupFileName=bname; showBackupDone=true;
                        statusMsg="Đã sao lưu phiên làm việc: " + bname; statusCol=SUCCESS_COLOR;
                        AddLog("SAO LƯU", "Backup: " + bname, true);
                    } else { statusMsg="LỖI: Tạo tệp sao lưu thất bại!"; statusCol=DANGER_COLOR; }
                }
            }

            if (showBackupDone) {
                TXT(("Tệp sao lưu an toàn: " + backupFileName).c_str(), CX, bY+96, 14, SUCCESS_COLOR);
            }

            int y = bY + 96 + (showBackupDone ? 22 : 0) + 14;

            DrawLabelBox(CX, y, CW, 46, "Số nguyên tố hệ thống p (Hex)",
                         gState.hasParams ? gState.params.p.toHex(true) : "Chưa khởi tạo...", TEXT_MAIN);
            y += LBH(15,46) + 18;

            DrawLabelBox(CX, y, CW, 46, "Số nguyên tố khóa q (Hex)",
                         gState.hasParams ? gState.params.q.toHex(true) : "Chưa khởi tạo...", TEXT_MAIN);
            y += LBH(15,46) + 18;

            DrawLabelBox(CX, y, CW, 46, "Phần tử sinh g (Bậc q modulo p)",
                         gState.hasParams ? gState.params.g.toHex(true) : "Chưa khởi tạo...", TEXT_MAIN);
            y += LBH(15,46) + 14;

            TXT("=> Kéo thả tệp .key hoặc .bak vào cửa sổ này để nạp lại dữ liệu phiên làm việc cũ.", CX, y, 14, GRAY);
            y += 28;

            HLine(y); y += 14;
            TXT("QUẢN LÝ DANH SÁCH KHÓA ĐÃ CẤP PHÁT", CX, y, 17, HEADER_BLUE); y += 28;

            DrawRectangle(CX, y, CW, 32, {225,238,255,255});
            TXT("STT",               CX+10, y+8, 15, HEADER_BLUE);
            TXT("Tên tệp Khóa Công Khai", CX+60, y+8, 15, HEADER_BLUE);
            TXT("Trạng thái",        CX+680, y+8, 15, HEADER_BLUE);
            TXT("Hành động",         CX+820, y+8, 15, HEADER_BLUE);
            y += 32;

            int rowH = 36, visRows = 4;
            int startI = gState.keyScrollY / rowH;
            for (int i = startI; i < (int)gState.savedKeyFiles.size() && i < startI+visRows; i++) {
                int ry = y + (i-startI)*rowH;
                DrawRectangle(CX, ry, CW, rowH-1, (i%2==0)?CARD_BG:ROW_ALT);
                DrawRectangleLines(CX, ry, CW, rowH-1, BORDER_COLOR);
                char nb[8]; snprintf(nb,8,"%d",i+1);
                TXT(nb, CX+16, ry+9, 15, TEXT_MAIN);
                std::string fn = gState.savedKeyFiles[i];
                if (fn.length()>60) fn=fn.substr(0,57)+"...";
                TXT(fn.c_str(), CX+60, ry+9, 14, TEXT_MAIN);
                if (gState.revokedKeys[i]) Badge(CX+680, ry+6, "ĐÃ THU HỒI", DANGER_COLOR, 13);
                else                       Badge(CX+680, ry+6, "ĐANG HOẠT ĐỘNG",  SUCCESS_COLOR, 13);
                if (!gState.revokedKeys[i] && !isProcessing)
                    if (Btn(CX+820, ry+5, 100, 26, "Thu hồi", false, DANGER_COLOR))
                        { confirmRevoke=true; confirmRevokeIdx=i; }
            }
            if (gState.savedKeyFiles.empty())
                TXT("(Chưa có khóa nào được xuất ra. Bấm 'LƯU KHÓA' để thêm.)", CX+40, y+12, 15, GRAY);

            if (confirmRevoke && confirmRevokeIdx >= 0) {
                DrawRectangle(CX+100, 250, 560, 150, WHITE);
                DrawRectangleLinesEx({(float)(CX+100),250,560,150}, 2, DANGER_COLOR);
                TXT("XÁC NHẬN THU HỒI CHỨNG CHỈ KHÓA?", CX+160, 270, 18, DANGER_COLOR);
                TXT("Hành động này sẽ đánh dấu khóa là không an toàn (Mô phỏng PKI).", CX+120, 308, 14, TEXT_MAIN);
                if (Btn(CX+120, 350, 160, 38, "XÁC NHẬN", false, DANGER_COLOR)) {
                    gState.revokedKeys[confirmRevokeIdx]=true;
                    statusMsg="Đã thu hồi quyền sử dụng khóa: " + gState.savedKeyFiles[confirmRevokeIdx];
                    statusCol=DANGER_COLOR;
                    AddLog("THU HỒI KHÓA", gState.savedKeyFiles[confirmRevokeIdx], true);
                    confirmRevoke=false; confirmRevokeIdx=-1;
                }
                if (Btn(CX+300, 350, 160, 38, "HỦY BỎ", false, Color{150,150,150,255}))
                    { confirmRevoke=false; confirmRevokeIdx=-1; }
            }
            break;
        }

        // ------------------------------------------------------------
        case KEY_SIGN: {
            SecHeader("TAB 2 - QUẢN LÝ KHÓA & THỰC HIỆN KÝ SỐ");

            if (!gState.hasParams) {
                TXT("[!] Vui lòng quay lại Tab 1 để tạo tham số hệ thống trước khi ký!", CX, TOP+10, 17, DANGER_COLOR);
                break;
            }

            int bY = TOP - 4;
            if (Btn(CX,       bY, 195, 42, "SINH CẶP KHÓA", false, BTN_TEAL) && !isProcessing) {
                auto [priv,pub] = DSA::generateKeyPair(gState.params);
                gState.privKey=priv; gState.pubKey=pub; gState.hasKeys=true;
                statusMsg="Đã sinh thành công cặp Khóa Bí mật và Khóa Công khai."; statusCol=SUCCESS_COLOR;
                AddLog("TẠO KHÓA", "Thành công", true);
            }
            if (gState.hasKeys && !isProcessing) {
                if (Btn(CX+205, bY, 195, 42, "LƯU KHÓA (.KEY)", false, WARNING_COLOR)) {
                    std::string pn,bn;
                    if (FileIOHelper::exportKeys(gState.params,gState.privKey,gState.pubKey,pn,bn)) {
                        savedPubName=bn; statusMsg="Đã xuất khóa: " + pn + " & " + bn; statusCol=SUCCESS_COLOR;
                        AddLog("LƯU KHÓA", bn, true);
                        gState.savedKeyFiles.push_back(bn); gState.revokedKeys.push_back(false);
                    } else { statusMsg="LỖI: Không thể ghi file ra ổ đĩa!"; statusCol=DANGER_COLOR; }
                }
                if (Btn(CX+410, bY, 195, 42, "SAO LƯU (.BAK)", false, {90,150,215,255})) {
                    std::string bname;
                    if (FileIOHelper::backupKeys(gState.params,gState.privKey,gState.pubKey,bname)) {
                        statusMsg="Đã tạo bản sao lưu phiên làm việc: " + bname; statusCol=SUCCESS_COLOR;
                        AddLog("SAO LƯU", "Backup: " + bname, true);
                    }
                }
            }

            int kw = (CW - 12) / 2;
            int y  = bY + 52 + 10;
            DrawLabelBox(CX,      y, kw, 44, "Khóa Bí mật x (Dùng để Ký)",
                         gState.hasKeys ? gState.privKey.x.toHex(true) : "Chưa tạo...", DANGER_COLOR);
            DrawLabelBox(CX+kw+12, y, kw, 44, "Khóa Công khai y (Dùng để Xác minh)",
                         gState.hasKeys ? gState.pubKey.y.toHex(true) : "Chưa tạo...", TEXT_HEADING);
            y += LBH(15,44) + 18;

            TXT("1. NHẬP VĂN BẢN TRỰC TIẾP (Gõ tiếng Việt không dấu):", CX, y, 15, TEXT_HEADING);
            y += 24;
            DrawRectangle(CX, y, CW-220, 44, CARD_BG);
            DrawRectangleLinesEx({(float)CX,(float)y,(float)(CW-220),44.0f}, 2, BORDER_COLOR);
            std::string disp = gState.textInput;
            if ((int)(GetTime()*2)%2==0 && !isProcessing) disp+="|";
            TXT(disp.c_str(), CX+12, y+12, 15, BLACK);
            if (Btn(CX+CW-212, y, 212, 44, "LƯU THÀNH FILE .TXT", false, HEADER_BLUE) && !isProcessing) {
                if (gState.textInputLen > 0) {
                    std::ofstream tf("vanban_tudao.txt");
                    if (tf.is_open()) { tf << gState.textInput; tf.close();
                        statusMsg="Đã lưu nội dung thành 'vanban_tudao.txt'!"; statusCol=SUCCESS_COLOR;
                    } else { statusMsg="LỖI: Không thể lưu file .txt!"; statusCol=DANGER_COLOR; }
                } else { statusMsg="LỖI: Ô nhập liệu đang trống!"; statusCol=DANGER_COLOR; }
            }
            y += 44 + 18;

            DropZone(CX, y, CW, 52, "2. HOẶC KÉO VÀ THẢ FILE (MỌI ĐỊNH DẠNG) VÀO ĐÂY ĐỂ KÝ:", gState.fileToSignPath);
            if (DropZoneClearBtn(CX+CW, y, 52, gState.fileToSignPath)) {
                gState.fileToSignPath.clear();
                statusMsg="Đã xoá file. Kéo thả file mới nếu cần."; statusCol=WARNING_COLOR;
            }
            y += DZH(52) + 18;

            if (gState.hasKeys && !isProcessing && asyncStatus == 0) {
                if (Btn(CX,     y, 190, 44, "KÝ VĂN BẢN", false, WARNING_COLOR)) {
                    if (gState.textInputLen == 0) {
                        statusMsg="LỖI TỪ CHỐI KÝ: Bạn chưa nhập nội dung văn bản!"; statusCol=DANGER_COLOR;
                    } else {
                        uint64 h = FileIOHelper::hashString(gState.textInput);
                        gState.signature = DSA::signHash(h, gState.privKey, gState.params);
                        gState.hasSignature=true; gState.signingText=true;
                        gState.lastSignedHash      = h;
                        gState.lastSignedPubKey    = gState.pubKey;
                        gState.lastSignedSignature = gState.signature;
                        gState.hasLastSignedInfo   = true;
                        statusMsg="Ký thành công! Bấm 'LƯU VĂN BẢN & CHỮ KÝ' để xuất file .sig ra máy."; statusCol=SUCCESS_COLOR;
                        AddLog("KÝ VĂN BẢN", "Hash=" + std::to_string(h), true);
                    }
                }
                if (!gState.fileToSignPath.empty()) {
                    if (Btn(CX+200, y, 170, 44, "KÝ FILE", false, BTN_TEAL)) {
                        asyncOperation=1;
                        std::thread t(FileIOHelper::hashFileAsync, gState.fileToSignPath); t.detach();
                    }
                }
                if (gState.hasSignature && gState.signingText) {
                    if (Btn(CX+380, y, 280, 44, "LƯU VĂN BẢN & CHỮ KÝ", false, SUCCESS_COLOR)) {
                        if (FileIOHelper::exportTextAndSignature(gState.textInput, gState.signature)) {
                            statusMsg="Đã đóng gói thành file 'vanban_ky.txt' và 'vanban_ky.txt.sig'."; statusCol=SUCCESS_COLOR;
                            AppState::SignedDoc sd;
                            sd.docName="vanban_ky.txt"; sd.sigFile="vanban_ky.txt.sig";
                            sd.keyFile=savedPubName.empty()?"chua_luu.key":savedPubName;
                            sd.timestamp=GetTimestamp();
                            gState.signedDocs.push_back(sd);
                            AddLog("LƯU TÀI LIỆU KÝ", "vanban_ky.txt", true);
                        } else { statusMsg="LỖI: Trình xuất file gặp sự cố!"; statusCol=DANGER_COLOR; }
                    }
                }
            } else if (isProcessing && asyncOperation==1) {
                TXT("Hệ thống đang tiến hành băm File...", CX, y+12, 15, TEXT_MAIN);
                DrawRectangleLines(CX, y+32, 520, 16, BORDER_COLOR);
                DrawRectangle(CX+1, y+33, (int)(518*asyncProgress.load()), 14, BTN_TEAL);
                TXT((std::to_string((int)(asyncProgress.load()*100))+"%").c_str(), CX+530, y+32, 15, TEXT_MAIN);
            } else if (!isProcessing && asyncOperation==1 && asyncStatus!=0) {
                if (asyncStatus==-1) { statusMsg="LỖI TỪ CHỐI KÝ: File rỗng (0 bytes)!"; statusCol=DANGER_COLOR; }
                else if (asyncStatus==-2) { statusMsg="LỖI IO: Không thể đọc file. File bị lỗi quyền truy cập."; statusCol=DANGER_COLOR; }
                else if (asyncStatus==1) {
                    gState.signature=DSA::signHash(asyncHashResult,gState.privKey,gState.params);
                    gState.hasSignature=true; gState.signingText=false;
                    FileIOHelper::exportSignature(gState.fileToSignPath, gState.signature);
                    gState.lastSignedHash      = asyncHashResult;
                    gState.lastSignedPubKey    = gState.pubKey;
                    gState.lastSignedSignature = gState.signature;
                    gState.hasLastSignedInfo   = true;
                    AppState::SignedDoc sd;
                    sd.docName=gState.fileToSignPath; sd.sigFile=gState.fileToSignPath+".sig";
                    sd.keyFile=savedPubName.empty()?"chua_luu.key":savedPubName;
                    sd.timestamp=GetTimestamp(); gState.signedDocs.push_back(sd);
                    statusMsg="KÝ THÀNH CÔNG! Đã đính kèm file chữ ký (.sig) cùng thư mục."; statusCol=SUCCESS_COLOR;
                    AddLog("KÝ FILE", gState.fileToSignPath, true);
                }
                asyncStatus=0; asyncOperation=0;
            }
            y += 44 + 18;

            DrawLabelBox(CX, y, CW, 44, "Thành phần chữ ký r (Hex)",
                         gState.hasSignature ? gState.signature.r.toHex(true) : "Chưa ký...", TEXT_MAIN);
            y += LBH(15,44) + 18;
            DrawLabelBox(CX, y, CW, 44, "Thành phần chữ ký s (Hex)",
                         gState.hasSignature ? gState.signature.s.toHex(true) : "Chưa ký...", TEXT_MAIN);
            y += LBH(15,44) + 14;

            HLine(y); y += 14;
            TXT("SỔ LƯU TRỮ TÀI LIỆU ĐÃ KÝ", CX, y, 17, HEADER_BLUE); y += 28;

            DrawRectangle(CX, y, CW, 32, {225,238,255,255});
            TXT("STT",        CX+8,   y+8, 14, HEADER_BLUE);
            TXT("Tên tài liệu",CX+55,  y+8, 14, HEADER_BLUE);
            TXT("Tệp .sig",  CX+480,  y+8, 14, HEADER_BLUE);
            TXT("Tệp .key",  CX+680,  y+8, 14, HEADER_BLUE);
            TXT("Thời gian",  CX+860,  y+8, 14, HEADER_BLUE);
            y += 32;

            int dRowH=34, dVis=3, dStart=gState.docScrollY/dRowH;
            for (int i=dStart; i<(int)gState.signedDocs.size() && i<dStart+dVis; i++) {
                int ry = y + (i-dStart)*dRowH;
                DrawRectangle(CX,ry,CW,dRowH-1,(i%2==0)?CARD_BG:ROW_ALT);
                DrawRectangleLines(CX,ry,CW,dRowH-1,BORDER_COLOR);
                auto& sd=gState.signedDocs[i];
                char nb[8]; snprintf(nb,8,"%d",i+1);
                TXT(nb, CX+14, ry+9, 14, TEXT_MAIN);
                std::string dn=sd.docName; if(dn.length()>38) dn=dn.substr(0,35)+"...";
                TXT(dn.c_str(), CX+55,  ry+9, 13, TEXT_MAIN);
                std::string sf=sd.sigFile; if(sf.length()>28) sf=sf.substr(0,25)+"...";
                TXT(sf.c_str(), CX+480, ry+9, 13, TEXT_MAIN);
                std::string kf=sd.keyFile; if(kf.length()>24) kf=kf.substr(0,21)+"...";
                TXT(kf.c_str(), CX+680, ry+9, 13, TEXT_MAIN);
                TXT(sd.timestamp.c_str(), CX+860, ry+9, 13, TEXT_MAIN);
            }
            if (gState.signedDocs.empty())
                TXT("(Chưa có tài liệu nào được ký trong phiên này. Hãy tiến hành Ký File.)", CX+60, y+10, 14, GRAY);
            break;
        }

        // ------------------------------------------------------------
        case VERIFY: {
            SecHeader("TAB 3 - ĐỐI CHIẾU VÀ XÁC MINH CHỮ KÝ (VERIFICATION)");

            int y = TOP - 4;
            TXT("Chọn giao thức kiểm tra dữ liệu gốc:", CX, y+12, 15, TEXT_MAIN);
            bool canClick = !isProcessing;
            if (Btn(CX+320, y, 185, 42, "Kiểm tra File thả vào", verifyMode==0, canClick ? BTN_TEAL : LIGHTGRAY) && canClick)
                { verifyMode=0; gState.lastVerifyResult=0; }
            if (Btn(CX+520, y, 265, 42, "Kiểm tra Văn bản nhập tay", verifyMode==1, canClick ? Color{90,150,215,255} : LIGHTGRAY) && canClick)
                { verifyMode=1; gState.lastVerifyResult=0; }
            y += 52 + 10;

            if (verifyMode == 0) {
                DropZone(CX, y, CW, 52, "1. FILE DỮ LIỆU GỐC (File được gửi kèm chữ ký):", gState.verifyFilePath);
                if (DropZoneClearBtn(CX+CW, y, 52, gState.verifyFilePath)) {
                    gState.verifyFilePath.clear(); gState.lastVerifyResult=0;
                    statusMsg="Đã xoá file gốc."; statusCol=WARNING_COLOR;
                }
                y += DZH(52) + 16;
                DropZone(CX, y, CW, 52, "2. FILE CHỮ KÝ BẢO MẬT (.sig):", gState.verifySigPath);
                if (DropZoneClearBtn(CX+CW, y, 52, gState.verifySigPath)) {
                    gState.verifySigPath.clear(); gState.lastVerifyResult=0;
                    statusMsg="Đã xoá file chữ ký (.sig)."; statusCol=WARNING_COLOR;
                }
                y += DZH(52) + 16;
                DropZone(CX, y, CW, 52, "3. FILE KHÓA CÔNG KHAI (.key):", gState.verifyPubKeyPath);
                if (DropZoneClearBtn(CX+CW, y, 52, gState.verifyPubKeyPath)) {
                    gState.verifyPubKeyPath.clear(); gState.lastVerifyResult=0;
                    statusMsg="Đã xoá file khóa công khai (.key)."; statusCol=WARNING_COLOR;
                }
                y += DZH(52) + 18;

                bool can = !gState.verifyFilePath.empty() && !gState.verifySigPath.empty() && !gState.verifyPubKeyPath.empty();
                if (can) {
                    if (!isProcessing && asyncStatus==0) {
                        if (Btn(CX, y, 350, 46, "TIẾN HÀNH XÁC MINH TOÀN VẸN", false, SUCCESS_COLOR)) {
                            asyncOperation=2;
                            std::thread t(FileIOHelper::hashFileAsync, gState.verifyFilePath); t.detach();
                        }
                    } else if (isProcessing && asyncOperation==2) {
                        TXT("Đang quét cấu trúc file...", CX, y+14, 15, TEXT_MAIN);
                        DrawRectangleLines(CX, y+34, 500, 16, BORDER_COLOR);
                        DrawRectangle(CX+1, y+35, (int)(498*asyncProgress.load()), 14, SUCCESS_COLOR);
                        TXT((std::to_string((int)(asyncProgress.load()*100))+"%").c_str(), CX+510, y+34, 15, TEXT_MAIN);
                    } else if (!isProcessing && asyncOperation==2 && asyncStatus!=0) {
                        if (asyncStatus < 0) {
                            if (asyncStatus == -1) {
                                statusMsg="LỖI IO: File gốc hoàn toàn trống rỗng (0 bytes)!"; statusCol=DANGER_COLOR;
                                AddLog("XÁC MINH", "File rỗng: " + gState.verifyFilePath, false); 
                                gState.lastVerifyResult = -11; 
                            } else {
                                statusMsg="LỖI IO: Không thể đọc File gốc (File bị hỏng/Lỗi quyền)!"; statusCol=DANGER_COLOR;
                                AddLog("XÁC MINH", "File hỏng: " + gState.verifyFilePath, false); 
                                gState.lastVerifyResult = -9;
                            }
                        } else {
                            DSAParams ep; PubKey epb; Signature es;
                            if (!FileIOHelper::loadPublicKey(gState.verifyPubKeyPath,ep,epb) ||
                                !FileIOHelper::loadSignature(gState.verifySigPath,es)) {
                                statusMsg="LỖI ĐỊNH DẠNG: Tệp .sig hoặc .key bị hỏng, chứa ký tự lạ!"; statusCol=WARNING_COLOR;
                                gState.lastVerifyResult=-2; AddLog("XÁC MINH", "Sai Format Khóa/Chữ ký", false);
                            } else {
                                bool ok = DSA::verifyHash(asyncHashResult, es, epb, ep);
                                bool revoked = isKeyRevoked(gState.verifyPubKeyPath);

                                if (revoked) {
                                    statusMsg="TỪ CHỐI: Khóa công khai đã bị THU HỒI! Chữ ký không đáng tin cậy."; statusCol=DANGER_COLOR;
                                    gState.lastVerifyResult = -10;
                                    AddLog("XÁC MINH FAIL", "Khóa đã bị thu hồi: " + gState.verifyPubKeyPath, false);
                                } 
                                else if (ok) {
                                    statusMsg="THÀNH CÔNG: Chữ ký HỢP LỆ! File hoàn toàn nguyên vẹn."; statusCol=SUCCESS_COLOR;
                                    gState.lastVerifyResult=1; AddLog("XÁC MINH (OK)", gState.verifyFilePath, true);
                                } else {
                                    int diag = DSA::diagnoseFailure(
                                        asyncHashResult, es, epb, ep,
                                        gState.lastSignedHash,
                                        gState.lastSignedSignature,
                                        gState.lastSignedPubKey,
                                        gState.hasLastSignedInfo);
                                    gState.lastVerifyResult = diag;
                                    
                                    if (diag == -1) {
                                        statusMsg="Nội dung file/văn bản đã bị sửa đổi! Chỉ dữ liệu bị can thiệp."; statusCol=DANGER_COLOR;
                                        AddLog("XAC MINH FAIL", "Du lieu bi sua: " + gState.verifyFilePath, false);
                                    } else if (diag == -7) {
                                        statusMsg="Chữ ký (.sig) và Khóa public (.key) đã bị sửa!"; statusCol=DANGER_COLOR;
                                        AddLog("XAC MINH FAIL", "Chu ky + Khoa bi sua: " + gState.verifyFilePath, false);
                                    } else if (diag == -5) {
                                        statusMsg="File chữ ký (.sig) đã bị sửa đổi hoặc làm giả!"; statusCol=DANGER_COLOR;
                                        AddLog("XAC MINH FAIL", "Chu ky bi sua: " + gState.verifyFilePath, false);
                                    } else if (diag == -3) {
                                        statusMsg="Khóa công khai (.key) không khớp! Có thể dùng khóa giả mạo."; statusCol=WARNING_COLOR;
                                        AddLog("XAC MINH FAIL", "Khoa gia mao: " + gState.verifyFilePath, false);
                                    } else if (diag == -6) {
                                        statusMsg="Cả dữ liệu lẫn file chữ ký đều đã bị sửa đổi!"; statusCol=DANGER_COLOR;
                                        AddLog("XAC MINH FAIL", "Van ban + chu ky bi sua: " + gState.verifyFilePath, false);
                                    } else if (diag == -8) {
                                        statusMsg="Dữ liệu và Khóa công khai đều đã bị sửa đổi!"; statusCol=DANGER_COLOR;
                                        AddLog("XAC MINH FAIL", "Van ban + khoa bi sua: " + gState.verifyFilePath, false);
                                    } else if (diag == -4) {
                                        statusMsg="Nghiêm trọng: Cả 3 thành phần (dữ liệu, chữ ký, khóa) đều bị sửa!"; statusCol=DANGER_COLOR;
                                        AddLog("XAC MINH FAIL", "Ca 3 deu bi sua: " + gState.verifyFilePath, false);
                                    } else {
                                        statusMsg="Chữ ký không hợp lệ! Phát hiện dữ liệu sai lệch."; statusCol=DANGER_COLOR;
                                        AddLog("XAC MINH FAIL", gState.verifyFilePath, false);
                                    }
                                }
                            }
                        }
                        asyncStatus=0; asyncOperation=0;
                    }
                    y += 52 + 14;
                }
            } else {
                TXT("1. NHẬP LẠI VĂN BẢN CẦN KIỂM TRA ĐỐI CHIẾU:", CX, y, 15, TEXT_HEADING);
                y += 24;
                DrawRectangle(CX, y, CW, 48, CARD_BG);
                DrawRectangleLinesEx({(float)CX,(float)y,(float)CW,48.0f}, 2, BORDER_COLOR);
                std::string dv = gState.verifyTextInput;
                if ((int)(GetTime()*2)%2==0 && !isProcessing) dv+="|";
                TXT(dv.c_str(), CX+12, y+14, 15, BLACK);
                y += 48 + 16;

                DropZone(CX, y, CW, 52, "2. FILE CHỮ KÝ BẢO MẬT (.sig):", gState.verifySigPath);
                if (DropZoneClearBtn(CX+CW, y, 52, gState.verifySigPath)) {
                    gState.verifySigPath.clear(); gState.lastVerifyResult=0;
                    statusMsg="Đã xoá file chữ ký (.sig)."; statusCol=WARNING_COLOR;
                }
                y += DZH(52) + 16;
                DropZone(CX, y, CW, 52, "3. FILE KHÓA CÔNG KHAI (.key):", gState.verifyPubKeyPath);
                if (DropZoneClearBtn(CX+CW, y, 52, gState.verifyPubKeyPath)) {
                    gState.verifyPubKeyPath.clear(); gState.lastVerifyResult=0;
                    statusMsg="Đã xoá file khóa công khai (.key)."; statusCol=WARNING_COLOR;
                }
                y += DZH(52) + 18;

                bool can2 = gState.verifyTextInputLen > 0 &&
                            !gState.verifySigPath.empty() &&
                            !gState.verifyPubKeyPath.empty();
                if (can2) {
                    if (Btn(CX, y, 350, 46, "XÁC MINH NỘI DUNG VĂN BẢN", false, SUCCESS_COLOR) && !isProcessing) {
                        DSAParams ep; PubKey epb; Signature es;
                        if (!FileIOHelper::loadPublicKey(gState.verifyPubKeyPath,ep,epb) ||
                            !FileIOHelper::loadSignature(gState.verifySigPath,es)) {
                            statusMsg="LỖI ĐỊNH DẠNG: Tệp .sig hoặc .key sai cấu trúc thuật toán!"; statusCol=WARNING_COLOR;
                            gState.lastVerifyResult=-2; AddLog("XÁC MINH", "Format error", false);
                        } else {
                            uint64 h = FileIOHelper::hashString(gState.verifyTextInput);
                            bool ok = DSA::verifyHash(h, es, epb, ep);
                            bool revoked = isKeyRevoked(gState.verifyPubKeyPath);

                            if (revoked) {
                                statusMsg="TỪ CHỐI: Khóa công khai đã bị THU HỒI! Chữ ký không đáng tin cậy."; statusCol=DANGER_COLOR;
                                gState.lastVerifyResult = -10;
                                AddLog("XÁC MINH FAIL", "Khóa đã bị thu hồi", false);
                            }
                            else if (ok) {
                                statusMsg="THÀNH CÔNG: Chữ ký HỢP LỆ! Văn bản khớp hoàn toàn."; statusCol=SUCCESS_COLOR;
                                gState.lastVerifyResult=1; AddLog("XÁC MINH (OK)", "Văn bản nhập tay", true);
                            } else {
                                int diag = DSA::diagnoseFailure(
                                    h, es, epb, ep,
                                    gState.lastSignedHash,
                                    gState.lastSignedSignature,
                                    gState.lastSignedPubKey,
                                    gState.hasLastSignedInfo);
                                gState.lastVerifyResult = diag;
                                if (diag == -1) {
                                    statusMsg="Nội dung file/văn bản đã bị sửa đổi! Chỉ dữ liệu bị can thiệp."; statusCol=DANGER_COLOR;
                                    AddLog("XAC MINH FAIL", "Van ban bi sua", false);
                                } else if (diag == -7) {
                                    statusMsg="Chữ ký (.sig) và Khóa public (.key) đã bị sửa!"; statusCol=DANGER_COLOR;
                                    AddLog("XAC MINH FAIL", "Chu ky + Khoa bi sua", false);
                                } else if (diag == -5) {
                                    statusMsg="File chữ ký (.sig) đã bị sửa đổi hoặc làm giả!"; statusCol=DANGER_COLOR;
                                    AddLog("XAC MINH FAIL", "Chu ky bi sua (van ban)", false);
                                } else if (diag == -3) {
                                    statusMsg="Khóa công khai (.key) không khớp! Có thể dùng khóa giả mạo."; statusCol=WARNING_COLOR;
                                    AddLog("XAC MINH FAIL", "Khoa gia mao (van ban)", false);
                                } else if (diag == -6) {
                                    statusMsg="Cả dữ liệu lẫn file chữ ký đều đã bị sửa đổi!"; statusCol=DANGER_COLOR;
                                    AddLog("XAC MINH FAIL", "Van ban + chu ky bi sua", false);
                                } else if (diag == -8) {
                                    statusMsg="Dữ liệu và Khóa công khai đều đã bị sửa đổi!"; statusCol=DANGER_COLOR;
                                    AddLog("XAC MINH FAIL", "Van ban + khoa bi sua (van ban)", false);
                                } else if (diag == -4) {
                                    statusMsg="Nghiêm trọng: Cả 3 thành phần (dữ liệu, chữ ký, khóa) đều bị sửa!"; statusCol=DANGER_COLOR;
                                    AddLog("XAC MINH FAIL", "Ca 3 deu bi sua (van ban)", false);
                                } else {
                                    statusMsg="Chữ ký không hợp lệ! Phát hiện dữ liệu sai lệch."; statusCol=DANGER_COLOR;
                                    AddLog("XAC MINH FAIL", "Van ban nhap tay", false);
                                }
                            }
                        }
                    }
                    y += 52 + 14;
                }
            }

            DrawRectangle(CX, y, CW, 150, CARD_BG);
            DrawRectangleLines(CX, y, CW, 150, BORDER_COLOR);
            TXT("BẢNG BÁO CÁO KẾT QUẢ XÁC MINH:", CX+14, y+12, 18, TEXT_HEADING);

            auto RRow = [&](int ry, const char* icon, const char* msg, Color c) {
                TXT(icon, CX+20, ry, 15, c);
                TXT(msg,  CX+56, ry, 15, c);
            };
            if (gState.lastVerifyResult == 1) {
                DrawRectangle(CX+8, y+40, CW-16, 100, {228,255,238,255});
                DrawRectangleLinesEx({(float)(CX+8),(float)(y+40),(float)(CW-16),100.0f}, 2, SUCCESS_COLOR);
                DrawRectangle(CX+8, y+40, 6, 100, SUCCESS_COLOR);
                TXT("[XAC THUC THANH CONG]  Chu ky so HOAN TOAN HOP LE", CX+22, y+46, 18, {10,110,50,255});
                RRow(y+72,  "[OK]", "Noi dung du lieu nguyen ven 100%, chua tung bi can thiep hay sua doi.", {20,130,60,255});
                RRow(y+93,  "[OK]", "Chu ky (r, s) khop chinh xac voi Khoa cong khai - Tinh xac thuc dam bao.", {20,130,60,255});
                RRow(y+114, "[OK]", "Nguoi ky khong the phu nhan trach nhiem (Tinh chong choi bo).", {20,130,60,255});
            }
            else if (gState.lastVerifyResult == -10) {
                DrawRectangle(CX+8, y+40, CW-16, 100, {255,230,230,255});
                DrawRectangleLinesEx({(float)(CX+8),(float)(y+40),(float)(CW-16),100.0f}, 2, DANGER_COLOR);
                DrawRectangle(CX+8, y+40, 6, 100, DANGER_COLOR);
                TXT("[KHÓA BỊ THU HỒI]  Chứng chỉ Khóa công khai đã bị hủy bỏ!", CX+22, y+46, 18, {175,30,30,255});
                RRow(y+72,  "[!!]", "Khóa này đang nằm trong Danh sách Thu hồi Chứng chỉ (CRL).", {175,30,30,255});
                RRow(y+93,  "[!!]", "Dù toán học có khớp, văn bản này vẫn BỊ TỪ CHỐI vì lý do bảo mật.", {175,30,30,255});
                RRow(y+114, "-->", "Biện pháp: Yêu cầu người gửi cung cấp Cặp khóa mới đang hoạt động.", TEXT_MAIN);
            }
            else if (gState.lastVerifyResult == -1) {
                DrawRectangle(CX+8, y+40, CW-16, 100, {255,228,228,255});
                DrawRectangleLinesEx({(float)(CX+8),(float)(y+40),(float)(CW-16),100.0f}, 2, DANGER_COLOR);
                DrawRectangle(CX+8, y+40, 6, 100, DANGER_COLOR);
                TXT("[NỘI DUNG BỊ SỬA ĐỔI]  Văn bản/file đã bị thay đổi sau khi ký!", CX+22, y+46, 18, {175,30,30,255});
                RRow(y+72,  "[!!]", "Chỉ nội dung bị sửa - Chữ ký (.sig) và Khóa (.key) vẫn nguyên vẹn.", {175,30,30,255});
                RRow(y+93,  "[!!]", "Khóa hợp lệ nhưng dữ liệu bị chỉnh sửa trái phép sau khi đã ký.", {155,40,40,255});
                RRow(y+114, "-->", "Biện pháp: Yêu cầu người gửi cung cấp lại file gốc chưa qua chỉnh sửa.", TEXT_MAIN);
            }
            else if (gState.lastVerifyResult == -7) {
                DrawRectangle(CX+8, y+40, CW-16, 100, {255,230,230,255});
                DrawRectangleLinesEx({(float)(CX+8),(float)(y+40),(float)(CW-16),100.0f}, 2, DANGER_COLOR);
                DrawRectangle(CX+8, y+40, 6, 100, {180,30,30,255});
                TXT("[CHỮ KÝ VÀ KHÓA BỊ SỬA]  Chữ ký và Khóa public đã bị sửa!", CX+22, y+46, 18, {160,0,0,255});
                RRow(y+72,  "[!!]", "Dữ liệu giữ nguyên nhưng file .sig và .key KHÔNG KHỚP với bản gốc.", {160,0,0,255});
                RRow(y+93,  "[!!]", "Phát hiện hành vi đánh tráo đồng thời cặp Khóa và Chữ ký số.", {160,0,0,255});
                RRow(y+114, "-->", "Biện pháp: Yêu cầu người gửi cung cấp lại Khóa public chính thống.", TEXT_MAIN);
            }
            else if (gState.lastVerifyResult == -5) {
                DrawRectangle(CX+8, y+40, CW-16, 100, {255,228,228,255});
                DrawRectangleLinesEx({(float)(CX+8),(float)(y+40),(float)(CW-16),100.0f}, 2, DANGER_COLOR);
                DrawRectangle(CX+8, y+40, 6, 100, DANGER_COLOR);
                TXT("[CHỮ KÝ BỊ SỬA ĐỔI]  File .sig đã bị chỉnh sửa hoặc làm giả!", CX+22, y+46, 18, {175,30,30,255});
                RRow(y+72,  "[!!]", "Chỉ file .sig bị sửa - Nội dung và Khóa (.key) vẫn nguyên vẹn.", {175,30,30,255});
                RRow(y+93,  "[!!]", "Giá trị (r, s) trong file .sig không khớp với chữ ký gốc.", {155,40,40,255});
                RRow(y+114, "-->", "Biện pháp: Yêu cầu người gửi xuất lại file .sig từ hệ thống chính thống.", TEXT_MAIN);
            }
            else if (gState.lastVerifyResult == -3) {
                DrawRectangle(CX+8, y+40, CW-16, 100, {255,240,215,255});
                DrawRectangleLinesEx({(float)(CX+8),(float)(y+40),(float)(CW-16),100.0f}, 2, WARNING_COLOR);
                DrawRectangle(CX+8, y+40, 6, 100, WARNING_COLOR);
                TXT("[KHÓA GIẢ MẠO]  Khóa công khai (.key) KHÔNG KHỚP với khóa dùng để ký!", CX+22, y+46, 18, {150,75,0,255});
                RRow(y+72,  "[!!]", "Chỉ khóa bị đổi - Nội dung và chữ ký (.sig) vẫn nguyên vẹn.", {150,75,0,255});
                RRow(y+93,  "[!!]", "Dấu hiệu tấn công: bên thứ ba dùng khóa khác thay thế khóa gốc.", {150,75,0,255});
                RRow(y+114, "-->", "Biện pháp: Liên hệ người ký xác minh lại Khóa công khai chính thống.", TEXT_MAIN);
            }
            else if (gState.lastVerifyResult == -6) {
                DrawRectangle(CX+8, y+40, CW-16, 100, {255,220,220,255});
                DrawRectangleLinesEx({(float)(CX+8),(float)(y+40),(float)(CW-16),100.0f}, 2, DANGER_COLOR);
                DrawRectangle(CX+8, y+40, 6, 100, {180,20,20,255});
                TXT("[VĂN BẢN + CHỮ KÝ BỊ SỬA]  Cả nội dung lẫn file .sig đều đã bị chỉnh sửa!", CX+22, y+46, 18, {160,0,0,255});
                RRow(y+72,  "[!!]", "Nội dung BỊ SỬA ĐỔI và file .sig cùng BỊ SỬA ĐỔI - Khóa (.key) vẫn đúng.", {160,0,0,255});
                RRow(y+93,  "[!!]", "Hai thành phần bị tấn công - danh tính người ký vẫn xác định được.", {160,0,0,255});
                RRow(y+114, "-->", "Biện pháp: Không sử dụng. Yêu cầu người gửi ký lại từ đầu.", TEXT_MAIN);
            }
            else if (gState.lastVerifyResult == -8) {
                DrawRectangle(CX+8, y+40, CW-16, 100, {255,220,220,255});
                DrawRectangleLinesEx({(float)(CX+8),(float)(y+40),(float)(CW-16),100.0f}, 2, DANGER_COLOR);
                DrawRectangle(CX+8, y+40, 6, 100, {180,20,20,255});
                TXT("[VĂN BẢN + KHÓA BỊ SỬA]  Cả nội dung lẫn Khóa công khai đều bị chỉnh sửa!", CX+22, y+46, 18, {160,0,0,255});
                RRow(y+72,  "[!!]", "Nội dung BỊ SỬA ĐỔI và Khóa (.key) BỊ ĐỔI - File .sig vẫn nguyên bản.", {160,0,0,255});
                RRow(y+93,  "[!!]", "Phát hiện đánh tráo khóa công khai kết hợp chỉnh sửa dữ liệu.", {160,0,0,255});
                RRow(y+114, "-->", "Biện pháp: Không sử dụng. Xác minh lại khóa và yêu cầu file gốc từ nguồn tin cậy.", TEXT_MAIN);
            }
            else if (gState.lastVerifyResult == -4) {
                DrawRectangle(CX+8, y+40, CW-16, 100, {255,215,215,255});
                DrawRectangleLinesEx({(float)(CX+8),(float)(y+40),(float)(CW-16),100.0f}, 2, DANGER_COLOR);
                DrawRectangle(CX+8, y+40, 6, 100, {160,0,0,255});
                TXT("[TẤT CẢ BỊ SỬA]  Toàn bộ 3 thành phần đều đã bị chỉnh sửa!", CX+22, y+46, 18, {150,0,0,255});
                RRow(y+72,  "[!!]", "Dữ liệu, chữ ký (.sig) VÀ Khóa công khai (.key) đều bị can thiệp.", {150,0,0,255});
                RRow(y+93,  "[!!]", "Toàn bộ hệ thống truyền tin bị xâm phạm - không xác định được nguồn gốc.", {150,0,0,255});
                RRow(y+114, "-->", "Biện pháp: KHÔNG sử dụng. Báo cáo sự cố bảo mật ngay lập tức.", TEXT_MAIN);
            }
            else if (gState.lastVerifyResult == -11) {
                DrawRectangle(CX+8, y+40, CW-16, 100, {255,248,220,255});
                DrawRectangleLinesEx({(float)(CX+8),(float)(y+40),(float)(CW-16),100.0f}, 2, WARNING_COLOR);
                DrawRectangle(CX+8, y+40, 6, 100, WARNING_COLOR);
                TXT("[FILE TRỐNG]  Tệp dữ liệu gốc hoàn toàn trống rỗng!", CX+22, y+46, 18, {140,75,0,255});
                RRow(y+72,  "[!!]", "Hệ thống phát hiện file gốc có dung lượng 0 bytes.", {140,75,0,255});
                RRow(y+93,  "[!!]", "Không thể xác minh chữ ký trên một file không có nội dung.", {140,75,0,255});
                RRow(y+114, "-->", "Biện pháp: Kéo thả lại file gốc có chứa dữ liệu hợp lệ.", TEXT_MAIN);
            }
            else if (gState.lastVerifyResult == -9) {
                DrawRectangle(CX+8, y+40, CW-16, 100, {255,230,230,255});
                DrawRectangleLinesEx({(float)(CX+8),(float)(y+40),(float)(CW-16),100.0f}, 2, DANGER_COLOR);
                DrawRectangle(CX+8, y+40, 6, 100, DANGER_COLOR);
                TXT("[FILE BỊ HỎNG]  Không thể đọc được cấu trúc file gốc!", CX+22, y+46, 18, {175,30,30,255});
                RRow(y+72,  "[!!]", "Tệp dữ liệu gốc bị hỏng (corrupted) hoặc bị HĐH khóa quyền đọc.", {175,30,30,255});
                RRow(y+93,  "[!!]", "Hệ thống không thể trích xuất các byte dữ liệu để tiến hành băm.", {175,30,30,255});
                RRow(y+114, "-->", "Biện pháp: Tải lại file từ nguồn gốc hoặc cấp quyền truy cập cho phần mềm.", TEXT_MAIN);
            }
            else if (gState.lastVerifyResult == -2) {
                DrawRectangle(CX+8, y+40, CW-16, 100, {255,248,220,255});
                DrawRectangleLinesEx({(float)(CX+8),(float)(y+40),(float)(CW-16),100.0f}, 2, WARNING_COLOR);
                DrawRectangle(CX+8, y+40, 6, 100, WARNING_COLOR);
                TXT("[LỖI ĐỌC KHÓA/CHỮ KÝ]  Tệp .sig hoặc .key bị sai định dạng", CX+22, y+46, 18, {140,75,0,255});
                RRow(y+72,  "[!!]", "Tệp .sig hoặc .key chứa ký tự không hợp lệ (không phải số nguyên DSA).", {140,75,0,255});
                RRow(y+93,  "[!!]", "Nội dung tệp có thể đã bị can thiệp bằng Text Editor dẫn đến lỗi cấu trúc.", {140,75,0,255});
                RRow(y+114, "-->", "Biện pháp: Kiểm tra lại tệp .sig và .key, dùng đúng file xuất từ Tab 2.", TEXT_MAIN);
            }
            else {
                TXT("(Chưa có dữ liệu. Vui lòng nạp đủ 3 tệp và bấm nút Xác minh ở trên.)", CX+20, y+65, 15, GRAY);
            }
            y += 150 + 16;

            DrawRectangle(CX, y, CW, 168, {247,250,255,255});
            DrawRectangleLines(CX, y, CW, 168, {200,220,240,255});
            TXT("SƠ ĐỒ TOÁN HỌC XÁC THỰC BÊN DƯỚI HỆ THỐNG (BACK-END):", CX+14, y+12, 16, TEXT_HEADING);
            TXT("B1. Băm nội dung file gửi tới => lấy mã Hash mới H(m)'.", CX+20, y+40,  15, TEXT_MAIN);
            TXT("B2. Tính giá trị nghịch đảo w = s^-1 mod q",                 CX+20, y+64,  15, TEXT_MAIN);
            TXT("B3. Giải phương trình u1 = H(m)' * w mod q   và   u2 = r * w mod q", CX+20, y+88,  15, TEXT_MAIN);
            TXT("B4. Chạy hàm lũy thừa siêu lớn v = [ (g^u1 * y^u2) mod p ] mod q",   CX+20, y+112, 15, TEXT_MAIN);
            TXT("B5. Hệ thống ra quyết định: Nếu v == r => CHẤP NHẬN        Nếu v != r => TỪ CHỐI", CX+20, y+136, 15, {40,120,80,255});
            break;
        }

        // ------------------------------------------------------------
        case THEORY: {
            SecHeader("TAB 4 - LÝ THUYẾT & CƠ CHẾ BẢO MẬT CỦA DSA");
            int y = TOP + 4;
            TXT("1. Tổng quan về DSA (Digital Signature Algorithm)", CX, y, 18, HEADER_BLUE); y+=30;
            TXT("- Tiêu chuẩn chữ ký số do Viện NIST Hoa Kỳ đề xuất năm 1991 (Chuẩn FIPS 186).", CX+16, y, 15, TEXT_MAIN); y+=24;
            TXT("- Độ an toàn tuyệt đối dựa trên sự phức tạp của bài toán Logarit rời rạc (Discrete Logarithm).", CX+16, y, 15, TEXT_MAIN); y+=24;
            TXT("- DSA chuyên dụng cho Ký số và Xác thực, KHÔNG có chức năng mã hóa bảo mật văn bản như RSA.", CX+16, y, 15, TEXT_MAIN); y+=32;

            auto TheoryBox=[&](const char* title, std::vector<const char*> lines) {
                int bh = 32 + (int)lines.size()*26 + 10;
                DrawRectangle(CX, y, CW, bh, CARD_BG);
                DrawRectangleLines(CX, y, CW, bh, BORDER_COLOR);
                DrawRectangle(CX, y, 5, bh, HEADER_BLUE);
                TXT(title, CX+18, y+10, 16, TEXT_HEADING);
                int ly = y+36;
                for (auto* l : lines) { TXT(l, CX+22, ly, 15, TEXT_MAIN); ly+=26; }
                y += bh + 14;
            };

            TheoryBox("[Giai đoạn 1] - Khởi tạo Hệ thống & Cặp Khóa", {
                "- Chọn: p (số nguyên tố siêu lớn), q (ước của p-1), g (phần tử sinh).",
                "- Khóa bí mật: x (máy tính chọn ngẫu nhiên, ẩn danh tuyệt đối).",
                "- Khóa công khai: y = g^x mod p (phân phối tự do trên mạng)."
            });
            TheoryBox("[Giai đoạn 2] - Thuật toán Ký dữ liệu (Tạo ra cặp r và s)", {
                "- Bước 1: Băm tệp tin (Hash) để lấy chuỗi nhận dạng H(m). Chọn biến ngẫu nhiên k.",
                "- Bước 2: Máy tính giải phương trình r = (g^k mod p) mod q.",
                "- Bước 3: Tính s = [k^-1 * (H(m) + x*r)] mod q.",
                "- Đầu ra cuối cùng là File chữ ký chứa hai con số (r, s)."
            });
            TheoryBox("[Giai đoạn 3] - Thuật toán Đối chiếu (Dùng Khóa công khai y)", {
                "- Hệ thống đích sẽ băm lại tệp tin vừa nhận để tự tính H(m)'.",
                "- Tính w = s^-1 mod q.",
                "- Dùng w, r và H(m)' để tìm ra các biến trung gian u1, u2.",
                "- Tính biến v thông qua phương trình hàm mũ khổng lồ v = [(g^u1 * y^u2) mod p] mod q.",
                "- Tính quyết định: Nếu v == r => Chữ ký HỢP LỆ! Nếu v lệch dù chỉ 1 đơn vị => TỪ CHỐI."
            });

            TXT("2. Các đặc tính bảo mật không thể phá vỡ của DSA", CX, y, 18, HEADER_BLUE); y+=30;
            TXT("- Tính Toàn vẹn (Integrity): Hacker sửa 1 byte file gốc => H(m) đổi => v khác r => Hệ thống từ chối ngay lập tức.", CX+16, y, 15, TEXT_MAIN); y+=24;
            TXT("- Tính Chống chối bỏ (Non-repudiation): Chỉ người giữ Khóa bí mật (x) mới nhào nặn ra được ma trận (r,s).", CX+16, y, 15, TEXT_MAIN); y+=24;
            TXT("- Tính Xác thực (Authentication): Bất kỳ ai tải Khóa công khai (y) cũng có thể làm giám khảo xác minh tài liệu.", CX+16, y, 15, TEXT_MAIN); y+=30;

            DrawRectangle(CX, y, CW, 50, {255,240,235,255});
            DrawRectangleLinesEx({(float)CX,(float)y,(float)CW,50.0f}, 2, DANGER_COLOR);
            TXT("[LỖ HỔNG CHÍ MẠNG] Biến ngẫu nhiên k bắt buộc phải sinh mới và tiêu hủy cho TỪNG LẦN ký.", CX+16, y+10, 15, DANGER_COLOR);
            TXT("Nếu hacker phát hiện bạn dùng một số k để ký 2 file khác nhau => x (Khóa bí mật) lập tức bị lộ hoàn toàn!", CX+16, y+30, 14, DANGER_COLOR);
            break;
        }

        // ------------------------------------------------------------
        case TAB_LOG: {
            SecHeader("TAB 5 - NHẬT KÝ KIỂM TOÁN HỆ THỐNG (SYSTEM LOGS)");
            int y = TOP - 4;

            if (Btn(CX, y, 220, 42, "XUẤT LOG RA FILE .TXT", false, HEADER_BLUE)) {
                if (FileIOHelper::exportLog(gState.logs)) {
                    statusMsg="Đã xuất báo cáo lịch sử thành file .txt an toàn!"; statusCol=SUCCESS_COLOR;
                } else { statusMsg="LỖI: Trình kết xuất file bị chặn!"; statusCol=DANGER_COLOR; }
            }
            if (Btn(CX+230, y, 175, 42, "XÓA TOÀN BỘ LOG", false, DANGER_COLOR)) {
                gState.logs.clear(); gState.logScrollY=0;
                statusMsg="CẢNH BÁO: Đã xóa sổ hoàn toàn nhật ký hệ thống!"; statusCol=WARNING_COLOR;
            }
            int cntOK=0, cntFail=0;
            for (auto& e : gState.logs) { if (e.success) cntOK++; else cntFail++; }
            Badge(CX+440, y+8, ("TỔNG SỐ: " + std::to_string(gState.logs.size())).c_str(), TEXT_SUB, 14);
            Badge(CX+570, y+8, ("THÀNH CÔNG: " + std::to_string(cntOK)).c_str(), SUCCESS_COLOR, 14);
            Badge(CX+730, y+8, ("THẤT BẠI: " + std::to_string(cntFail)).c_str(), DANGER_COLOR, 14);
            y += 52 + 8;

            DrawRectangle(CX, y, CW, 34, {225,238,255,255});
            TXT("STT",        CX+8,   y+9, 14, HEADER_BLUE);
            TXT("Mốc Thời Gian",  CX+55,  y+9, 14, HEADER_BLUE);
            TXT("Mã Lệnh Thao Tác",  CX+230, y+9, 14, HEADER_BLUE);
            TXT("Trạng Thái", CX+380, y+9, 14, HEADER_BLUE);
            TXT("Ghi Chú Kỹ Thuật",   CX+490, y+9, 14, HEADER_BLUE);
            y += 34;

            int logRowH=34, visLog=(STATUS_Y-y-16)/logRowH;
            int startL=gState.logScrollY/logRowH;
            for (int i=startL; i<(int)gState.logs.size() && i<startL+visLog; i++) {
                int ry = y + (i-startL)*logRowH;
                DrawRectangle(CX,ry,CW,logRowH-1,(i%2==0)?CARD_BG:ROW_ALT);
                DrawRectangleLines(CX,ry,CW,logRowH-1,BORDER_COLOR);
                auto& e=gState.logs[i];
                char nb[8]; snprintf(nb,8,"%d",i+1);
                TXT(nb,              CX+14,  ry+9, 14, TEXT_MAIN);
                TXT(e.timestamp.c_str(), CX+55, ry+9, 13, TEXT_MAIN);
                TXT(e.action.c_str(),    CX+230, ry+9, 14, TEXT_HEADING);
                if (e.success) Badge(CX+380, ry+6, "OK",   SUCCESS_COLOR, 13);
                else           Badge(CX+380, ry+6, "LỖI", DANGER_COLOR,  13);
                std::string det=e.detail; if(det.length()>55) det=det.substr(0,52)+"...";
                TXT(det.c_str(), CX+490, ry+9, 13, TEXT_MAIN);
            }
            if (gState.logs.empty())
                TXT("(Chưa có biến động bảo mật nào được ghi nhận trong phiên này.)", CX+60, y+16, 15, GRAY);

            if ((int)gState.logs.size() > visLog && visLog > 0) {
                int sbX=VW-18, sbY=y, sbH=visLog*logRowH;
                DrawRectangle(sbX, sbY, 8, sbH, {210,210,210,255});
                int maxSY=std::max(1, (int)gState.logs.size()*logRowH - sbH);
                int tH=std::max(30, sbH*visLog/(int)gState.logs.size());
                int tY=sbY+(int)((float)gState.logScrollY/maxSY*(sbH-tH));
                DrawRectangle(sbX, tY, 8, tH, BTN_TEAL);
            }
            break;
        }
        } 

        rlPopMatrix();
        EndDrawing();
    }

    UnloadTexture(logoHaUI);
    UnloadFont(appFont);
    CloseWindow();
    return 0;
}