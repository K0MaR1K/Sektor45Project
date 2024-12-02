package com.payten.ecrdemo.ecr;

import java.util.List;

public class EcrJsonRsp {
    public Header header;
    public Response response;

    public static class Header{
        public int length;
        public String hash;
        public String version;
    }

    public static class Printer{
        public String errorCode;
    }

    public  static class Apdu{
        public String result;
        public String data;
    }

    public static class Commands{
        public Printer printer;
        public Apdu apdu;
    }

    public static class Result{
        public String code;
        public String message;
    }

    public static class Card{
        public String name;
        public String cardInterface;
        public String bin;
        public String pan;
        public String hashedPAN;
    }

    public static class Id{
        public Card card;
        public String authorization;
        public String terminal;
        public String merchant;
        public String invoice;
        public String ecr;
        public String batch;
        public String reference;
        public String sequenceNumber;
        public String controlNumber;
    }

    public static class Amounts{
        public String base;
        public String cashback;
        public String tip;
        public String fee;
        public String discount;
        public String currencyCode;
        public String due;
        public String first;
        public String subs;
        public String balance;
        public String total;
    }

    public static class Cvm{
        public String signature;
        public String pinOnline;
        public String pinOffline;
        public String pinOfflinePlain;
    }

    public static class Flags{
        public Cvm cvm;
    }

    public static class Cryptogram1{
        public String type;
        public String value;
    }

    public static class Cryptogram2{
        public String type;
        public String value;
    }

    public static class Emv{
        public String accountType;
        public String aid;
        public String aidLabel;
        public String aosa;
        public Cryptogram1 cryptogram1;
        public Cryptogram2 cryptogram2;
        public String currencyCode;
        public String tvr;
        public String ttq;
        public String ctq;
    }

    public static class Balance{
        public String sign;
        public String amount;
        public String currencyCode;
    }

    public static class Installments{
        public String rate;
        public String fee;
        public String apr;
        public String delay;
        public Amounts amounts;
        public String count;
        public String paymentMsg;
        public String moreInfoMsg;
        public String accToIssCondMsg;
    }

    public static class Dcc{
        public Amounts amounts;
        public String exchangeRate;
        public String rateScale;
        public String dateTime;
        public String commission;
        public String markUp;
        public String disclaimer;
    }

    public static class Services{
        public Installments installments;
        public Dcc dcc;
    }

    public static class Total{
        public String creditCount;
        public String credit;
        public String creditRevCount;
        public String creditRev;
        public String debitCount;
        public String debit;
        public String debitRevCount;
        public String debitRev;
        public String totalCount;
        public String total;
        public String currencyCode;
    }

    public static class Transaction{
        public String dateTime;
        public String transaction;
        public String amount;
        public String currencyCode;
        public Id id;
    }

    public static class Issuer{
        public String name;
        public List<Total> totals;
        public List<Transaction> transactions;
    }

    public static class Acquirer{
        public String name;
        public String tid;
        public String mid;
        public String batch;
        public String dateTime;
        public List<Total> totals;
        public List<Issuer> issuers;
    }

    public static class OverallTotals{
        public List<Acquirer> acquirers;
    }

    public static class Financial{
        public String transaction;
        public Result result;
        public Id id;
        public String dateTime;
        public Amounts amounts;
        public Flags flags;
        public Emv emv;
        public Balance balance;
        public Services services;
        public OverallTotals overallTotals;
    }

    public static class AcquirerData{
        public String acquirerName;
        public String acquirerTid;
        public String acquirerMid;
    }

    public static class Config{
        public String profileId;
        public String serialNumber;
        public String applicationVersion;
        public List<AcquirerData> acquirerData;
    }

    public static class Response{
        public Commands commands;
        public Financial financial;
        public Status status;
        public Config config;
    }

    public static class Status{
        public String code;
        public String message;
    }
}
