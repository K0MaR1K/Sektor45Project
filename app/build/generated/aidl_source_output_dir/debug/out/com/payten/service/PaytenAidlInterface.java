/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package com.payten.service;
public interface PaytenAidlInterface extends android.os.IInterface
{
  /** Default implementation for PaytenAidlInterface. */
  public static class Default implements com.payten.service.PaytenAidlInterface
  {
    @Override public java.lang.String ecrResponse(java.lang.String ecrRequest) throws android.os.RemoteException
    {
      return null;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.payten.service.PaytenAidlInterface
  {
    private static final java.lang.String DESCRIPTOR = "com.payten.service.PaytenAidlInterface";
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.payten.service.PaytenAidlInterface interface,
     * generating a proxy if needed.
     */
    public static com.payten.service.PaytenAidlInterface asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.payten.service.PaytenAidlInterface))) {
        return ((com.payten.service.PaytenAidlInterface)iin);
      }
      return new com.payten.service.PaytenAidlInterface.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      switch (code)
      {
        case INTERFACE_TRANSACTION:
        {
          reply.writeString(descriptor);
          return true;
        }
        case TRANSACTION_ecrResponse:
        {
          data.enforceInterface(descriptor);
          java.lang.String _arg0;
          _arg0 = data.readString();
          java.lang.String _result = this.ecrResponse(_arg0);
          reply.writeNoException();
          reply.writeString(_result);
          return true;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
    }
    private static class Proxy implements com.payten.service.PaytenAidlInterface
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      @Override public java.lang.String ecrResponse(java.lang.String ecrRequest) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(ecrRequest);
          boolean _status = mRemote.transact(Stub.TRANSACTION_ecrResponse, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            return getDefaultImpl().ecrResponse(ecrRequest);
          }
          _reply.readException();
          _result = _reply.readString();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      public static com.payten.service.PaytenAidlInterface sDefaultImpl;
    }
    static final int TRANSACTION_ecrResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    public static boolean setDefaultImpl(com.payten.service.PaytenAidlInterface impl) {
      // Only one user of this interface can use this function
      // at a time. This is a heuristic to detect if two different
      // users in the same process use this function.
      if (Stub.Proxy.sDefaultImpl != null) {
        throw new IllegalStateException("setDefaultImpl() called twice");
      }
      if (impl != null) {
        Stub.Proxy.sDefaultImpl = impl;
        return true;
      }
      return false;
    }
    public static com.payten.service.PaytenAidlInterface getDefaultImpl() {
      return Stub.Proxy.sDefaultImpl;
    }
  }
  public java.lang.String ecrResponse(java.lang.String ecrRequest) throws android.os.RemoteException;
}
