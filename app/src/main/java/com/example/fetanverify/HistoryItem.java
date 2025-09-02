package com.example.fetanverify;

import android.os.Parcel;
import android.os.Parcelable;

public class HistoryItem implements Parcelable {
    private String transactionId;
    private String status;
    private String timestamp;
    private String amount;
    private String sender;
    private String verifiedBy;
    private String userEmail;

    public HistoryItem(String transactionId, String status, String timestamp, String amount, String sender, String verifiedBy, String userEmail) {
        this.transactionId = transactionId;
        this.status = status;
        this.timestamp = timestamp;
        this.amount = amount;
        this.sender = sender;
        this.verifiedBy = verifiedBy;
        this.userEmail = userEmail;
    }

    // Constructor with verifiedBy but no userEmail (for backward compatibility)
    public HistoryItem(String transactionId, String status, String timestamp, String amount, String sender, String verifiedBy) {
        this(transactionId, status, timestamp, amount, sender, verifiedBy, null);
    }

    // Constructor with amount but no sender (for backward compatibility)
    public HistoryItem(String transactionId, String status, String timestamp, String amount) {
        this(transactionId, status, timestamp, amount, null, null, null);
    }

    // Legacy constructor for backward compatibility
    public HistoryItem(String transactionId, String status, String timestamp) {
        this(transactionId, status, timestamp, "N/A", null, null, null);
    }

    protected HistoryItem(Parcel in) {
        transactionId = in.readString();
        status = in.readString();
        timestamp = in.readString();
        amount = in.readString();
        sender = in.readString();
        verifiedBy = in.readString();
        userEmail = in.readString();
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

    public String getSender() {
        return sender != null ? sender : "N/A";
    }

    public String getVerifiedBy() {
        return verifiedBy != null ? verifiedBy : "N/A";
    }

    public String getUserEmail() {
        return userEmail != null ? userEmail : "N/A";
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
        dest.writeString(sender);
        dest.writeString(verifiedBy);
        dest.writeString(userEmail);
    }
}