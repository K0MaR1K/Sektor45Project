package com.payten.ecrdemo.ecr;

import java.util.ArrayList;
import java.util.List;

public class EcrJsonReq {
    public Header header;
    public Request request;

    public static class Header{
        public int length;
        public String hash;
        public String version;
    }

    public static class Apdu{
        public String action;
        public String destination;
        public String data;
    }

    public static class Printer{
        public String type;
        public String data;
        public ArrayList<PrintLines> printLines;
    }

    public static class PrintLines {
        public String type;
        public String style;
        public String content;
    }

    public static class Command{
        public Printer printer;
        public Apdu apdu;
    }

    public static class Custom{
        public String type;
        public String value;
    }

    public static class Id{
        public String invoice;
        public String ecr;
        public String authorization;
        public String reference;
        public String sequenceNumber;
        public String controlNumber;
        public String acquirer;
        public String cardName;
        public String cashier;
        public List<Custom> custom;
    }

//    public static class Original{
//        public String approvalCode;
//        public String reference;
//    }

    public static class Amounts{
        public String base;
        public String cashback;
        public String tip;
        public String original;
        public String currencyCode;
        public String due;
        public String first;
        public String subs;
    }

    public static class Options{
        public String language;
        public String print;
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

    public static class Services{
        public Installments installments;
    }

    public static class Financial{
        public String transaction;
        public Id id;
        //public Original original;
        public Amounts amounts;
        public Options options;
        public Services services;
    }

    public static class Status{

    }

    public static class Request{
        public Command command;
        public Financial financial;
        public Status status;
    }
}
