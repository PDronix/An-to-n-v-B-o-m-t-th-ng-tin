package dsa;

import dsa.core.log.ActivityLog;
import dsa.core.param.DSAKeyPair;
import dsa.core.param.DSAParameter;
import dsa.core.sign.DSASignature;
import dsa.core.sign.DSASigner;
import dsa.core.verify.DSAVerifier;
import java.io.File;
/**
 * AppContext – Trạng thái dùng chung toàn ứng dụng.
 * Được truyền vào tất cả các Panel để chia sẻ dữ liệu.
 */
public class AppContext {

    // Tham số và khóa
    public final DSAParameter param   = new DSAParameter();
    public final DSAKeyPair   keyPair = new DSAKeyPair();

    // Chữ ký gần nhất
    public DSASignature lastSignature = null;

    // Nhật ký
    public final ActivityLog log = new ActivityLog();

    // Thống kê nhanh
    public int     totalSigned   = 0;
    public int     totalVerified = 0;
    public boolean keySaved        = false;  // da luu khoa ra file chua
    public boolean privateKeySaved = false;  // da luu khoa bi mat chua
    public boolean publicKeySaved  = false;  // da luu khoa cong khai chua
    // ── BỔ SUNG: Biến lưu vết lịch sử sinh file từ hệ thống ──────────────
    public File sysPublicDsakeyFile = null; // Gán giá trị khi lưu file thành công ở tab Tạo tham số
    public File sysGocFile          = null; // Gán giá trị khi chọn file gốc ký ở tab Tạo chữ kỳ
    public File sysSigFile          = null; // Gán giá trị khi xuất file .sig ở tab Tạo chữ ký
    public String sysGocText        = null; // Gán giá trị khi nhấn Ký Văn Bản ở tab Tạo Chữ Ký
    // ── Truy vấn trạng thái ──────────────────────────────────────────────

    public boolean hasParams()    { return param.isReady(); }
    public boolean hasKeys()      { return keyPair.isReady(); }
    public boolean hasSignature() { return lastSignature != null; }

    public String statusString() {
        return (hasParams()    ? "[OK] Tham so san sang\n"   : "[--] Chua co tham so\n")
             + (hasKeys()      ? "[OK] Cap khoa san sang\n"  : "[--] Chua co khoa\n")
             + (hasSignature() ? "[OK] Co chu ky gan nhat\n" : "[--] Chua ky tai lieu nao\n");
    }

    // ── Factory methods ───────────────────────────────────────────────────

    public DSASigner  newSigner()   { return new DSASigner(param, keyPair); }
    public DSAVerifier newVerifier() { return new DSAVerifier(param, keyPair); }

    public void sysP() {

    }
}

