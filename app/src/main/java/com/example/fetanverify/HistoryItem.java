package com.example.fetanverify;

import android.os.Parcel;
import android.os.Parcelable;

public class HistoryItem implements Parcelable {
    private String transactionId;
    private String status;
    private String timestamp;
    private String amount;

    public HistoryItem(String transactionId, String status, String timestamp, String amount) {
        this.transactionId = transactionId;
        this.status = status;
        this.timestamp = timestamp;
        this.amount = amount;
    }

    // Legacy constructor for backward compatibility
    public HistoryItem(String transactionId, String status, String timestamp) {
        this(transactionId, status, timestamp, "N/A");
    }

    protected HistoryItem(Parcel in) {
        transactionId = in.readString();
        status = in.readString();
        timestamp = in.readString();
        amount = in.readString();
    }

    public static final Creator<HistoryItem> CREATOR = new Creator<HistoryItem>() {
        @Override
        public HistoryItem createFromParcel(Parcel in) {
            return new HistoryItem(in);
        }

        @Override
        public HistoryItem[] newArray(int size) {
            return new HistoryItem[size];
        }
    };

    public String getTransactionId() {
        return transactionId;
    }

    public String getStatus() {
        return status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getAmount() {
        return amount != null ? amount : "N/A";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(transactionId);
        dest.writeString(status);
        dest.writeString(timestamp);
        dest.writeString(amount);
    }
}