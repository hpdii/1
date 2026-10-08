package com.donk.app.models;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

public class PageResponse<T> {

    @SerializedName(value = "items", alternate = {"content"})
    private List<T> items;

    @SerializedName("page")
    private int page;

    @SerializedName("size")
    private int size;

    @SerializedName(value = "totalItems", alternate = {"totalElements"})
    private long totalItems;

    @SerializedName(value = "hasMore", alternate = {"hasNext"})
    private boolean hasMore;

    public List<T> getItems() {
        return items != null ? items : Collections.emptyList();
    }

    public List<T> getContent() {
        return getItems();
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(long totalItems) {
        this.totalItems = totalItems;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }
}
