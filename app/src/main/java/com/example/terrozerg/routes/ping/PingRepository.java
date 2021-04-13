package com.example.terrozerg.routes.ping;

import android.os.Handler;

import java.net.InetAddress;
import java.util.concurrent.Executor;

//class for handling internet connection check
public class PingRepository {
    private final Executor executor;
    private final Handler handler;

    public PingRepository(Executor executor, Handler handler){
        this.executor = executor;
        this.handler = handler;
    }

    //inet check
    private boolean check(){
        try {
            InetAddress address = InetAddress.getByName("google.com");
            return !address.equals("");

        } catch (Exception e) {
            return false;
        }
    }

    //run ping request from main thread
    public void request(final internetCheckCallback callback){
        executor.execute(new Runnable() {
            @Override
            public void run() {
                boolean result = check();
                notifyResult(callback, result);
            }
        });
    }

    //notify main thread
    private void notifyResult(final internetCheckCallback callback, final boolean result){
        handler.post(new Runnable() {
            @Override
            public void run() {
                callback.onComplete(result);
            }
        });
    }

}
