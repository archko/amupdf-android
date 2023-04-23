package cn.archko.pdf.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.radaee.comm.Global;
import com.radaee.pdf.Document;
import com.radaee.util.FileBrowserAdt;
import com.radaee.util.FileBrowserView;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cn.archko.mupdf.R;
import cn.archko.pdf.AppExecutors;
import cn.archko.pdf.common.PDFCreaterHelper;
import cn.archko.pdf.fragments.CreatePdfFragment;
import cn.archko.pdf.utils.PDFUtilities;

public class PDFToolActivity extends FragmentActivity implements PDFUtilities.OnOperationListener {//, View.OnClickListener {


    public static void start(@NotNull Context context) {
        context.startActivity(new Intent(context, PDFToolActivity.class));
    }

    private Document mDestDoc = null;
    private Document mSrcDoc = null;
    private ProgressDialog mProgressDialog;
    private final int SHOW_PROGRESS_DIALOG = 0;
    private final int DISMISS_PROGRESS_DIALOG = 1;
    private RecyclerView.Adapter<ToolHolder> adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Global.Init(this);

        setContentView(R.layout.fragment_pdf_tool);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        List<Item> items = new ArrayList<>();
        items.add(new Item(getString(R.string.merge_pdf_label), R.drawable.ic_merge_pdf, Item.TYPE_MERGE));
        //items.add(new Item(getString(R.string.convert_pdf_label), R.drawable.ic_convert_pdf, Item.TYPE_CONVERT));
        items.add(new Item(getString(R.string.encrypt_pdf_label), R.drawable.ic_encryption, Item.TYPE_ENCRYPT));
        items.add(new Item(getString(R.string.decrypt_pdf_label), R.drawable.ic_decryption, Item.TYPE_DECRYPT));
        items.add(new Item(getString(R.string.compress_pdf_label), R.drawable.ic_compress_pdf, Item.TYPE_COMPRESS));
        items.add(new Item(getString(R.string.convert_pdfa_label), R.drawable.ic_convert_pdfa, Item.TYPE_PDFA));
        items.add(new Item(getString(R.string.create_pdf_label), R.drawable.ic_convert_pdfa, Item.TYPE_CREATE_PDF));

        final LayoutInflater inflater = LayoutInflater.from(this);
        adapter = new RecyclerView.Adapter<ToolHolder>() {
            @NonNull
            @Override
            public ToolHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = inflater.inflate(R.layout.pdf_tool_item, parent, false);
                return new ToolHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull ToolHolder holder, int position) {
                ToolHolder toolHolder = (ToolHolder) holder;
                toolHolder.bind(items.get(position));
            }

            @Override
            public int getItemCount() {
                return items.size();
            }
        };
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onDone(Object result, int requestCode) {
        mHandler.sendEmptyMessage(DISMISS_PROGRESS_DIALOG);
        this.runOnUiThread(() -> {
            if (requestCode == PDFUtilities.REQUEST_CODE_MERGE_PDF || requestCode == PDFUtilities.REQUEST_CODE_CONVERT_PDF) {
                //PDFReaderActivity.ms_tran_doc = (Document) result;
                //Intent intent = new Intent(this, PDFReaderActivity.class);
                //intent.putExtra("data_source", "app");
                //startActivity(intent);
                Toast.makeText(this, "not implemented.", Toast.LENGTH_SHORT).show();
            } else if (requestCode == PDFUtilities.REQUEST_CODE_ENCRYPT_PDF) {
                String path = (String) result;
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.encrypt_success_hint, path));
                builder.setPositiveButton(R.string.button_ok_label, (dialog, which) -> dialog.dismiss());
                builder.create().show();
            } else if (requestCode == PDFUtilities.REQUEST_CODE_DECRYPT_PDF) {
                String path = (String) result;
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.decrypt_success_hint, path));
                builder.setPositiveButton(R.string.button_ok_label, (dialog, which) -> dialog.dismiss());
                builder.create().show();
            } else if (requestCode == PDFUtilities.REQUEST_CODE_COMPRESS_PDF) {
                String path = (String) result;
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.compress_success_hint, path));
                builder.setPositiveButton(R.string.button_ok_label, (dialog, which) -> dialog.dismiss());
                builder.setCancelable(false);
                builder.create().show();
            } else if (requestCode == PDFUtilities.REQUEST_CODE_CONVERT_PDFA) {
                String path = (String) result;
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.convert_success_hint, path));
                builder.setPositiveButton(R.string.button_ok_label, (dialog, which) -> dialog.dismiss());
                builder.create().show();
            }
        });
    }

    @Override
    public void onError(String error, int requestCode) {
        mHandler.sendEmptyMessage(DISMISS_PROGRESS_DIALOG);

        this.runOnUiThread(() -> {
            if (requestCode == PDFUtilities.REQUEST_CODE_ENCRYPT_PDF)
                Toast.makeText(this, R.string.encrypt_failed_hint, Toast.LENGTH_LONG).show();
            else if (requestCode == PDFUtilities.REQUEST_CODE_COMPRESS_PDF)
                Toast.makeText(this, R.string.compress_failed_hint, Toast.LENGTH_LONG).show();
            else if (requestCode == PDFUtilities.REQUEST_CODE_CONVERT_PDFA)
                Toast.makeText(this, R.string.convert_failed_hint, Toast.LENGTH_LONG).show();
        });
    }

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("HandlerLeak")
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_PROGRESS_DIALOG:
                    mProgressDialog = new ProgressDialog(PDFToolActivity.this);
                    mProgressDialog.setMessage(getResources().getString(R.string.message_wait_label));
                    mProgressDialog.setCancelable(false);
                    mProgressDialog.show();
                    break;
                case DISMISS_PROGRESS_DIALOG:
                    removeMessages(SHOW_PROGRESS_DIALOG);
                    if (mProgressDialog != null && mProgressDialog.isShowing())
                        mProgressDialog.dismiss();
                    mProgressDialog = null;
                    break;
                default:
                    break;
            }
        }
    };

    private final View.OnClickListener mEncryptPDFClickListener = v -> {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(getLayoutInflater().inflate(R.layout.dialog_pick_file, null));
        AlertDialog dlg = builder.create();
        dlg.setOnShowListener(dialog -> {
            FileBrowserView fb_view = dlg.findViewById(R.id.fb_view);
            TextView txt_filter = dlg.findViewById(R.id.extension_filter);
            txt_filter.setText("*.pdf");
            fb_view.FileInit(Environment.getExternalStorageDirectory().getPath(), new String[]{".pdf"});
            fb_view.setOnItemClickListener((parent, view1, position, id1) -> {
                FileBrowserAdt.SnatchItem item = (FileBrowserAdt.SnatchItem) fb_view.getItemAtPosition(position);
                if (item.m_item.is_dir())
                    fb_view.FileGotoSubdir(item.m_item.get_name());
                else {
                    Document pdfDoc = new Document();
                    String fullPath = item.m_item.get_path();
                    int ret = pdfDoc.Open(fullPath, "");
                    if (ret == 0) {
                        InputPswd(fullPath, pdfDoc, null, PDFUtilities.REQUEST_CODE_ENCRYPT_PDF);
                    } else if (ret == -1) {
                        InputPswd(fullPath, pdfDoc, null, PDFUtilities.REQUEST_CODE_ENCRYPT_PDF);
                    }
                    dlg.dismiss();
                }
            });
        });
        dlg.show();
    };

    private final View.OnClickListener mDecryptPDFClickListener = v -> {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(getLayoutInflater().inflate(R.layout.dialog_pick_file, null));
        AlertDialog dlg = builder.create();
        dlg.setOnShowListener(dialog -> {
            FileBrowserView fb_view = dlg.findViewById(R.id.fb_view);
            TextView txt_filter = dlg.findViewById(R.id.extension_filter);
            txt_filter.setText("*.pdf");
            fb_view.FileInit(Environment.getExternalStorageDirectory().getPath(), new String[]{".pdf"});
            fb_view.setOnItemClickListener((parent, view1, position, id1) -> {
                FileBrowserAdt.SnatchItem item = (FileBrowserAdt.SnatchItem) fb_view.getItemAtPosition(position);
                if (item.m_item.is_dir())
                    fb_view.FileGotoSubdir(item.m_item.get_name());
                else {
                    Document pdfDoc = new Document();
                    String fullPath = item.m_item.get_path();
                    int ret = pdfDoc.Open(fullPath, "");
                    if (ret == 0) {
                        Toast.makeText(this, R.string.not_encrypted_pdf_hint, Toast.LENGTH_LONG).show();
                    } else if (ret == -1) {
                        InputPswd(fullPath, pdfDoc, null, PDFUtilities.REQUEST_CODE_DECRYPT_PDF);
                    }
                    dlg.dismiss();
                }
            });
        });
        dlg.show();
    };

    private final View.OnClickListener mCompressPDFClickListener = v -> {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(getLayoutInflater().inflate(R.layout.dialog_pick_file, null));
        AlertDialog dlg = builder.create();
        dlg.setOnShowListener(dialog -> {
            FileBrowserView fb_view = dlg.findViewById(R.id.fb_view);
            TextView txt_filter = dlg.findViewById(R.id.extension_filter);
            txt_filter.setText("*.pdf");
            fb_view.FileInit(Environment.getExternalStorageDirectory().getPath(), new String[]{".pdf"});
            fb_view.setOnItemClickListener((parent, view1, position, id1) -> {
                FileBrowserAdt.SnatchItem item = (FileBrowserAdt.SnatchItem) fb_view.getItemAtPosition(position);
                if (item.m_item.is_dir())
                    fb_view.FileGotoSubdir(item.m_item.get_name());
                else {
                    Document pdfDoc = new Document();
                    String fullPath = item.m_item.get_path();
                    int ret = pdfDoc.Open(fullPath, "");
                    if (ret == 0) {
                        String path = fullPath.substring(0, fullPath.lastIndexOf("/"));
                        String name = fullPath.substring(fullPath.lastIndexOf("/") + 1, fullPath.lastIndexOf("."));
                        name = name + "_compressed.pdf";
                        path = path + File.separatorChar + name;
                        mHandler.sendEmptyMessage(SHOW_PROGRESS_DIALOG);
                        String finalPath = path;
                        Thread thread = new Thread(() -> PDFUtilities.CompressPDF(finalPath, PDFToolActivity.this, pdfDoc));
                        thread.start();
                    } else if (ret == -1) {
                        InputPswd(item.m_item.get_path(), mDestDoc, null, PDFUtilities.REQUEST_CODE_COMPRESS_PDF);
                    } else if (ret != 0) {
                        mDestDoc.Close();
                        mDestDoc = null;
                    }
                    dlg.dismiss();
                }
            });
        });
        dlg.show();
    };

    private final View.OnClickListener mConvertPDFAClickListener = v -> {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(getLayoutInflater().inflate(R.layout.dialog_pick_file, null));
        AlertDialog dlg = builder.create();
        dlg.setOnShowListener(dialog -> {
            FileBrowserView fb_view = dlg.findViewById(R.id.fb_view);
            TextView txt_filter = dlg.findViewById(R.id.extension_filter);
            txt_filter.setText("*.txt");
            fb_view.FileInit(Environment.getExternalStorageDirectory().getPath(), new String[]{".txt"});
            fb_view.setOnItemClickListener((parent, view1, position, id1) -> {
                FileBrowserAdt.SnatchItem item = (FileBrowserAdt.SnatchItem) fb_view.getItemAtPosition(position);
                if (item.m_item.is_dir())
                    fb_view.FileGotoSubdir(item.m_item.get_name());
                else {
                    String fullPath = item.m_item.get_path();
                    String path = fullPath.substring(0, fullPath.lastIndexOf("/"));
                    String name = fullPath.substring(fullPath.lastIndexOf("/") + 1, fullPath.lastIndexOf("."));
                    name = name + "_.pdf";
                    path = path + File.separatorChar + name;
                    mHandler.sendEmptyMessage(SHOW_PROGRESS_DIALOG);
                    String finalPath = path;
                    AppExecutors.Companion.getInstance().networkIO().execute(() -> {
                        boolean rs = PDFCreaterHelper.INSTANCE.createTextPage(fullPath, finalPath);
                        AppExecutors.Companion.getInstance().mainThread().execute(() -> {
                            if (rs) {
                                Toast.makeText(PDFToolActivity.this, "转换成功:" + finalPath, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(PDFToolActivity.this, "转换失败", Toast.LENGTH_LONG).show();
                            }
                            mHandler.sendEmptyMessage(DISMISS_PROGRESS_DIALOG);
                            dlg.dismiss();
                        });
                    });
                }
            });
        });
        dlg.show();
    };

    private final View.OnClickListener mConvertPDFClickListener = v -> {
        //PDFUtilities.ConvertDocxToPDF(this, this);
    };

    private final View.OnClickListener mOnMergePDFClickListener = v -> {
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_merge_pdf, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);
        Button destFileBtn = view.findViewById(R.id.btn_dest_file);
        destFileBtn.setOnClickListener(button -> {
            AlertDialog.Builder builder1 = new AlertDialog.Builder(view.getContext());
            builder1.setView(inflater.inflate(R.layout.dialog_pick_file, null));
            AlertDialog dlg = builder1.create();
            dlg.setOnShowListener(dialog -> {
                FileBrowserView fb_view = dlg.findViewById(R.id.fb_view);
                TextView txt_filter = dlg.findViewById(R.id.extension_filter);
                txt_filter.setText("*.pdf");
                fb_view.FileInit(Environment.getExternalStorageDirectory().getPath(), new String[]{".pdf"});
                fb_view.setOnItemClickListener((parent, view1, position, id1) -> {
                    FileBrowserAdt.SnatchItem item = (FileBrowserAdt.SnatchItem) fb_view.getItemAtPosition(position);
                    if (item.m_item.is_dir())
                        fb_view.FileGotoSubdir(item.m_item.get_name());
                    else {
                        if (mDestDoc != null) {
                            mDestDoc.Close();
                            mDestDoc = null;
                        }
                        mDestDoc = new Document();
                        int ret = mDestDoc.Open(item.m_item.get_path(), "");
                        if (ret == 0)
                            ((Button) button).setText(item.m_path);
                        else if (ret == -1) {
                            InputPswd(item.m_item.get_path(), mDestDoc, (Button) button, PDFUtilities.REQUEST_CODE_MERGE_PDF);
                        } else if (ret != 0) {
                            mDestDoc.Close();
                            mDestDoc = null;
                        }
                        dlg.dismiss();
                    }
                });
            });
            dlg.show();
        });
        Button sourceFileBtn = view.findViewById(R.id.btn_source_file);
        sourceFileBtn.setOnClickListener(button -> {
            AlertDialog.Builder builder2 = new AlertDialog.Builder(view.getContext());
            builder2.setView(inflater.inflate(R.layout.dialog_pick_file, null));
            AlertDialog dlg = builder2.create();
            dlg.setOnShowListener(dialog -> {
                FileBrowserView fb_view = dlg.findViewById(R.id.fb_view);
                TextView txt_filter = dlg.findViewById(R.id.extension_filter);
                txt_filter.setText("*.pdf");
                fb_view.FileInit(Environment.getExternalStorageDirectory().getPath(), new String[]{".pdf"});
                fb_view.setOnItemClickListener((parent, view1, position, id1) -> {
                    FileBrowserAdt.SnatchItem item = (FileBrowserAdt.SnatchItem) fb_view.getItemAtPosition(position);
                    if (item.m_item.is_dir())
                        fb_view.FileGotoSubdir(item.m_item.get_name());
                    else {
                        if (mSrcDoc != null) {
                            mSrcDoc.Close();
                            mSrcDoc = null;
                        }
                        mSrcDoc = new Document();
                        int ret = mSrcDoc.Open(item.m_item.get_path(), "");
                        if (ret == 0)
                            ((Button) button).setText(item.m_path);
                        else if (ret == -1) {
                            InputPswd(item.m_item.get_path(), mSrcDoc, ((Button) button), PDFUtilities.REQUEST_CODE_MERGE_PDF);
                        } else {
                            mSrcDoc.Close();
                            mSrcDoc = null;
                        }
                        dlg.dismiss();
                    }
                });
            });
            dlg.show();
        });
        builder.setPositiveButton(R.string.button_ok_label, (dialog, which) -> {
            if (mDestDoc == null || mSrcDoc == null)
                return;
            PDFUtilities.MergePDF(mDestDoc, mSrcDoc, this);
        });
        builder.setNegativeButton(R.string.button_cancel_label, (dialog, which) -> {
            dialog.dismiss();
        });
        builder.create().show();
    };

    private final View.OnClickListener mCreatePDFAClickListener = v -> {
        CreatePdfFragment.Companion.showCreateDialog(this, null);
        //PDFCreaterHelper.INSTANCE.save();
        /*AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(getLayoutInflater().inflate(R.layout.dialog_pick_file, null));
        AlertDialog dlg = builder.create();
        dlg.setOnShowListener(dialog -> {
            FileBrowserView fb_view = dlg.findViewById(R.id.fb_view);
            TextView txt_filter = dlg.findViewById(R.id.extension_filter);
            txt_filter.setText("*.jpg,*.jpeg,*.png");
            fb_view.FileInit(Environment.getExternalStorageDirectory().getPath(), new String[]{".jpg", ".jpeg", ".png"});
            fb_view.setOnItemClickListener((parent, view1, position, id1) -> {
                FileBrowserAdt.SnatchItem item = (FileBrowserAdt.SnatchItem) fb_view.getItemAtPosition(position);
                if (item.m_item.is_dir())
                    fb_view.FileGotoSubdir(item.m_item.get_name());
                else {
                    String fullPath = item.m_item.get_path();
                    String path = "/sdcard/book/new.pdf";
                    List<String> list = new ArrayList<>();
                    list.add(fullPath);
                    PDFCreaterHelper.INSTANCE.createPdf(path, list);
                    dlg.dismiss();
                }
            });
        });
        dlg.show();*/
    };

    private void InputPswd(String itemPath, Document document, Button button, int operationCode) {
        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(com.radaee.viewlib.R.layout.dlg_pswd, null);
        final EditText passwordInput = layout.findViewById(com.radaee.viewlib.R.id.txt_password);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            String password = passwordInput.getText().toString();
            if (operationCode == PDFUtilities.REQUEST_CODE_ENCRYPT_PDF) {
                int ret = document.Open(itemPath, password);
                if (ret == -1)
                    InputPswd(itemPath, document, button, operationCode);
                else if (ret == 0) {
                    String path = itemPath.substring(0, itemPath.lastIndexOf("/"));
                    String name = itemPath.substring(itemPath.lastIndexOf("/") + 1, itemPath.lastIndexOf("."));
                    name = name + "_encrypted.pdf";
                    path = path + File.separatorChar + name;
                    PDFUtilities.EncryptPDF(path, password, this, document);
                }
            } else if (operationCode == PDFUtilities.REQUEST_CODE_DECRYPT_PDF) {
                int ret = document.Open(itemPath, password);
                if (ret == -1)
                    InputPswd(itemPath, document, button, operationCode);
                else if (ret == 0) {
                    if (button != null && operationCode == PDFUtilities.REQUEST_CODE_MERGE_PDF)
                        button.setText(itemPath);
                    else if (operationCode == PDFUtilities.REQUEST_CODE_DECRYPT_PDF) {
                        String path = itemPath.substring(0, itemPath.lastIndexOf("/"));
                        String name = itemPath.substring(itemPath.lastIndexOf("/") + 1, itemPath.lastIndexOf("."));
                        name = name + "_decrypted.pdf";
                        path = path + File.separatorChar + name;
                        PDFUtilities.DecryptPDF(path, this, document);
                    }
                }
            } else if (operationCode == PDFUtilities.REQUEST_CODE_COMPRESS_PDF) {
                int ret = document.Open(itemPath, password);
                if (ret == -1)
                    InputPswd(itemPath, document, button, operationCode);
                else if (ret == 0) {
                    String path = itemPath.substring(0, itemPath.lastIndexOf("/"));
                    String name = itemPath.substring(itemPath.lastIndexOf("/") + 1, itemPath.lastIndexOf("."));
                    name = name + "_compressed.pdf";
                    path = path + File.separatorChar + name;
                    mHandler.sendEmptyMessage(SHOW_PROGRESS_DIALOG);
                    String finalPath = path;
                    Thread thread = new Thread(() -> PDFUtilities.CompressPDF(finalPath, PDFToolActivity.this, document));
                    thread.start();
                }
            } else if (operationCode == PDFUtilities.REQUEST_CODE_CONVERT_PDFA) {
                int ret = document.Open(itemPath, password);
                if (ret == -1)
                    InputPswd(itemPath, document, button, operationCode);
                else if (ret == 0) {
                    String path = itemPath.substring(0, itemPath.lastIndexOf("/"));
                    String name = itemPath.substring(itemPath.lastIndexOf("/") + 1, itemPath.lastIndexOf("."));
                    name = name + "_PDFA.pdf";
                    path = path + File.separatorChar + name;
                    mHandler.sendEmptyMessage(SHOW_PROGRESS_DIALOG);
                    String finalPath = path;
                    Thread thread = new Thread(() -> PDFUtilities.ConvertPDFA(finalPath, this, document));
                    thread.start();
                }
            }
            dialog.dismiss();
            return;
        });
        builder.setNegativeButton(com.radaee.viewlib.R.string.text_cancel_label, (dialog, which) -> dialog.dismiss());
        builder.setTitle(com.radaee.viewlib.R.string.input_password);
        builder.setCancelable(false);
        builder.setView(layout);

        AlertDialog dlg = builder.create();
        dlg.show();
    }

    private class ToolHolder extends RecyclerView.ViewHolder {

        private ImageView imageView;
        private TextView textView;

        public ToolHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.img_tool_item);
            textView = itemView.findViewById(R.id.txt_tool_item);
        }

        void bind(Item item) {
            textView.setText(item.text);
            imageView.setImageResource(item.resId);
            int type = item.type;
            if (type == Item.TYPE_MERGE) {
                itemView.setOnClickListener(mOnMergePDFClickListener);
            }
            if (type == Item.TYPE_CONVERT) {
                itemView.setOnClickListener(mConvertPDFClickListener);
            }
            if (type == Item.TYPE_ENCRYPT) {
                itemView.setOnClickListener(mEncryptPDFClickListener);
            }
            if (type == Item.TYPE_DECRYPT) {
                itemView.setOnClickListener(mDecryptPDFClickListener);
            }
            if (type == Item.TYPE_COMPRESS) {
                itemView.setOnClickListener(mCompressPDFClickListener);
            }
            if (type == Item.TYPE_PDFA) {
                itemView.setOnClickListener(mConvertPDFAClickListener);
            }
            if (type == Item.TYPE_CREATE_PDF) {
                itemView.setOnClickListener(mCreatePDFAClickListener);
            }
        }
    }

    private class Item {
        static final int TYPE_MERGE = 0;
        static final int TYPE_CONVERT = 1;
        static final int TYPE_ENCRYPT = 2;
        static final int TYPE_DECRYPT = 3;
        static final int TYPE_COMPRESS = 4;
        static final int TYPE_PDFA = 5;
        static final int TYPE_RENDER_VIEW = 6;
        static final int TYPE_CREATE_PDF = 7;
        public String text;
        public int resId;
        public int type;

        public Item(String text, int resId, int type) {
            this.text = text;
            this.resId = resId;
            this.type = type;
        }
    }
}
