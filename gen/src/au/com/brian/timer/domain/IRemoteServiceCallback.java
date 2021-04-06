/*___Generated_by_IDEA___*/

/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package au.com.brian.timer.domain;
/**
 * Example of a callback interface used by IRemoteService to send
 * synchronous notifications back to its clients.  Note that this is a
 * one-way interface so the server does not block waiting for the client.
 */
public interface IRemoteServiceCallback extends android.os.IInterface
{
  /** Default implementation for IRemoteServiceCallback. */
  public static class Default implements au.com.brian.timer.domain.IRemoteServiceCallback
  {
    /**
         * Called when the service has a new value for you.
         */
    @Override public void reloadRecipe() throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements au.com.brian.timer.domain.IRemoteServiceCallback
  {
    private static final java.lang.String DESCRIPTOR = "au.com.brian.timer.domain.IRemoteServiceCallback";
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an au.com.brian.timer.domain.IRemoteServiceCallback interface,
     * generating a proxy if needed.
     */
    public static au.com.brian.timer.domain.IRemoteServiceCallback asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof au.com.brian.timer.domain.IRemoteServiceCallback))) {
        return ((au.com.brian.timer.domain.IRemoteServiceCallback)iin);
      }
      return new au.com.brian.timer.domain.IRemoteServiceCallback.Stub.Proxy(obj);
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
        case TRANSACTION_reloadRecipe:
        {
          data.enforceInterface(descriptor);
          this.reloadRecipe();
          return true;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
    }
    private static class Proxy implements au.com.brian.timer.domain.IRemoteServiceCallback
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
      /**
           * Called when the service has a new value for you.
           */
      @Override public void reloadRecipe() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_reloadRecipe, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().reloadRecipe();
            return;
          }
        }
        finally {
          _data.recycle();
        }
      }
      public static au.com.brian.timer.domain.IRemoteServiceCallback sDefaultImpl;
    }
    static final int TRANSACTION_reloadRecipe = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    public static boolean setDefaultImpl(au.com.brian.timer.domain.IRemoteServiceCallback impl) {
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
    public static au.com.brian.timer.domain.IRemoteServiceCallback getDefaultImpl() {
      return Stub.Proxy.sDefaultImpl;
    }
  }
  /**
       * Called when the service has a new value for you.
       */
  public void reloadRecipe() throws android.os.RemoteException;
}
