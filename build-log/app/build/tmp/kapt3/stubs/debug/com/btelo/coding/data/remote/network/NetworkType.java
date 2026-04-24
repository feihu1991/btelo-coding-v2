package com.btelo.coding.data.remote.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import com.btelo.coding.util.Logger;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;
import javax.inject.Singleton;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0007\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005j\u0002\b\u0006j\u0002\b\u0007\u00a8\u0006\b"}, d2 = {"Lcom/btelo/coding/data/remote/network/NetworkType;", "", "(Ljava/lang/String;I)V", "WIFI", "CELLULAR", "ETHERNET", "OTHER", "NONE", "app_debug"})
public enum NetworkType {
    /*public static final*/ WIFI /* = new WIFI() */,
    /*public static final*/ CELLULAR /* = new CELLULAR() */,
    /*public static final*/ ETHERNET /* = new ETHERNET() */,
    /*public static final*/ OTHER /* = new OTHER() */,
    /*public static final*/ NONE /* = new NONE() */;
    
    NetworkType() {
    }
    
    @org.jetbrains.annotations.NotNull()
    public static kotlin.enums.EnumEntries<com.btelo.coding.data.remote.network.NetworkType> getEntries() {
        return null;
    }
}