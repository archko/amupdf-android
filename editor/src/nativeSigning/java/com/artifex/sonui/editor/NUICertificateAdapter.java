package com.artifex.sonui.editor;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;


public class NUICertificateAdapter extends RecyclerView.Adapter<NUICertificateAdapter.ViewHolder> {

    private ArrayList<NUICertificate> mCertificates;
    private LayoutInflater            mInflater;
    private ItemClickListener         mClickListener;
    private int                       selectedPos = RecyclerView.NO_POSITION;

    // data is passed into the constructor
    NUICertificateAdapter(Context context, ArrayList<NUICertificate> certs) {
        this.mInflater = LayoutInflater.from(context);
        this.mCertificates = certs;
    }

    // inflates the row layout from xml when needed
    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.sodk_editor_certificate_item, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the view and button in each cell
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < mCertificates.size()) {
            HashMap<String, String> details = mCertificates.get(position).designatedName();
            if (details != null)
                holder.myTextView.setText(details.get("CN"));
            else
                holder.myTextView.setText("-");

            holder.getItemView().setSelected(selectedPos == position);
        }
    }

    public int getSelectedPos() {
        return selectedPos;
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mCertificates.size();
    }

    public void selectItem(int pos) {
        super.notifyItemChanged(selectedPos);
        selectedPos = pos;
        super.notifyItemChanged(selectedPos);
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        com.artifex.sonui.editor.SOTextView myTextView;

        ViewHolder(View itemView) {
            super(itemView);
            myTextView = itemView.findViewById(R.id.sodk_editor_certificate_name);
            itemView.setOnClickListener(this);
        }

        public View getItemView() {
            return super.itemView;
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, super.getAdapterPosition());
            selectItem( super.getLayoutPosition() );
        }
    }

    // convenience method for getting data at click position
    public NUICertificate getItem(int id) {
        return mCertificates.get(id);
    }

    // allows clicks events to be caught
    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}
