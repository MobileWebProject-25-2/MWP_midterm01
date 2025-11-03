package com.example.imageviewdemo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Post> postList;
    private List<Post> postListFull; // 검색용 원본 리스트
    private OnItemClickListener onItemClickListener;
    private OnDeleteClickListener onDeleteClickListener;

    public interface OnItemClickListener {
        void onItemClick(Post post);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(Post post, int position);
    }

    public PostAdapter(List<Post> postList) {
        this.postList = postList;
        this.postListFull = new ArrayList<>(postList);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);
        holder.titleTextView.setText(post.getTitle());
        holder.imageView.setImageBitmap(post.getImageBitmap());

        holder.imageView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(post);
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(post, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    // 검색 필터
    public void filter(String query) {
        postList.clear();
        if (query.isEmpty()) {
            postList.addAll(postListFull);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Post post : postListFull) {
                if (post.getTitle().toLowerCase().contains(lowerCaseQuery)) {
                    postList.add(post);
                }
            }
        }
        notifyDataSetChanged();
    }

    // 정렬
    public void sortByDateDescending() {
        postList.sort((p1, p2) -> p2.getCreatedDate().compareTo(p1.getCreatedDate()));
        postListFull.sort((p1, p2) -> p2.getCreatedDate().compareTo(p1.getCreatedDate()));
        notifyDataSetChanged();
    }

    public void sortByDateAscending() {
        postList.sort((p1, p2) -> p1.getCreatedDate().compareTo(p2.getCreatedDate()));
        postListFull.sort((p1, p2) -> p1.getCreatedDate().compareTo(p2.getCreatedDate()));
        notifyDataSetChanged();
    }

    // 아이템 삭제
    public void removeItem(int position) {
        Post removedPost = postList.get(position);
        postList.remove(position);
        postListFull.remove(removedPost);
        notifyItemRemoved(position);
    }

    // 리스트 업데이트
    public void updateList(List<Post> newList) {
        this.postList.clear();
        this.postList.addAll(newList);
        this.postListFull.clear();
        this.postListFull.addAll(newList);
        notifyDataSetChanged();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleTextView;
        Button deleteButton;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewItem);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}

