//package cn.archko.pdf.utils;
//
//import android.app.Activity;
//import android.content.Context;
//import android.os.IBinder;
//import android.view.View;
//import android.view.inputmethod.InputMethodManager;
//
//import com.radaee.pdf.Document;
//
//import java.util.Random;
//
//public class PDFUtilities {
//    public interface OnOperationListener {
//        void onDone(Object result, int requestCode);
//
//        void onError(String error, int requestCode);
//    }
//
//    public static final int REQUEST_CODE_MERGE_PDF = 0;
//    public static final int REQUEST_CODE_CONVERT_PDF = 1;
//    public static final int REQUEST_CODE_ENCRYPT_PDF = 2;
//    public static final int REQUEST_CODE_DECRYPT_PDF = 3;
//    public static final int REQUEST_EXPORT_TO_HTML = 4;
//    public static final int REQUEST_CODE_COMPRESS_PDF = 5;
//    public static final int REQUEST_CODE_CONVERT_PDFA = 6;
//
//    public static void MergePDF(Document destDoc, Document sourceDoc, OnOperationListener listener) {
//        if (destDoc == null || sourceDoc == null) {
//            listener.onError(null, REQUEST_CODE_MERGE_PDF);
//        }
//
//        Document.ImportContext context = destDoc.ImportStart(sourceDoc);
//        int pageCount = sourceDoc.GetPageCount();
//        int startIndex = destDoc.GetPageCount();
//        for (int index = 0; index < pageCount; index++) {
//            context.ImportPage(index, startIndex);
//            startIndex++;
//        }
//        context.Destroy();
//        sourceDoc.Close();
//        if (destDoc.CanSave())
//            destDoc.Save();
//        listener.onDone(destDoc, REQUEST_CODE_MERGE_PDF);
//    }
//
//    public static void ExportToHTML(Document document, String path, OnOperationListener listener) {
//        Document.HtmlExportor exportor = document.NewHtmlExportor(path);
//        exportor.ExportEnd(false);
//        listener.onDone(null, REQUEST_EXPORT_TO_HTML);
//    }
//
//    public static void EncryptPDF(String path, String password, OnOperationListener listener, Document document) {
//        byte[] id = new byte[32];
//        new Random(0).nextBytes(id);
//        boolean ret = document.EncryptAs(path, password, password, 0x10, 3, id);
//        document.Close();
//        document = null;
//        if (ret)
//            listener.onDone(path, REQUEST_CODE_ENCRYPT_PDF);
//        else
//            listener.onError("", REQUEST_CODE_ENCRYPT_PDF);
//    }
//
//    public static void DecryptPDF(String path, OnOperationListener listener, Document document) {
//        Document destDoc = new Document();
//        destDoc.Create(path);
//        if (document == null) {
//            listener.onError(null, REQUEST_CODE_DECRYPT_PDF);
//        }
//
//        Document.ImportContext context = destDoc.ImportStart(document);
//        int pageCount = document.GetPageCount();
//        int startIndex = destDoc.GetPageCount();
//        for (int index = 0; index < pageCount; index++) {
//            context.ImportPage(index, startIndex);
//            startIndex++;
//        }
//        context.Destroy();
//        if (destDoc.CanSave())
//            destDoc.Save();
//        document.Close();
//        document = null;
//        destDoc.Close();
//        destDoc = null;
//        listener.onDone(path, REQUEST_CODE_DECRYPT_PDF);
//        //MergePDF(destDoc, document, listener);
//    }
//
//    public static void CompressPDF(String path, OnOperationListener listener, Document document) {
//        boolean ret = document.SaveAs(path, new byte[]{1, 1, 0, 2, 1, 0, 1, 0, 1, 0}, 144);
//        document.Close();
//        document = null;
//        if (ret)
//            listener.onDone(path, REQUEST_CODE_COMPRESS_PDF);
//        else
//            listener.onError("", REQUEST_CODE_COMPRESS_PDF);
//    }
//
//    public static void ConvertPDFA(String path, OnOperationListener listener, Document document) {
//        boolean ret = document.SaveAs(path, new byte[]{1, 1, 0, 2, 1, 0, 1, 0, 1, 1}, 144);
//        document.Close();
//        document = null;
//        if (ret)
//            listener.onDone(path, REQUEST_CODE_CONVERT_PDFA);
//        else
//            listener.onError("", REQUEST_CODE_CONVERT_PDFA);
//    }
//
//
//    public static void hideKeyboard(Context context) {
//        if (Activity.class.isInstance(context)) {
//            Activity activity = (Activity) context;
//            //  fix https://bugs.ghostscript.com/show_bug.cgi?id=702763
//            //  get the window token from the activity content
//            IBinder windowToken = activity.findViewById(android.R.id.content).getRootView().getWindowToken();
//            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
//            imm.hideSoftInputFromWindow(windowToken, 0);
//        }
//    }
//
//    public static void hideKeyboard(Context context, View view) {
//        if (view != null) {
//            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
//            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
//        }
//    }
//}
