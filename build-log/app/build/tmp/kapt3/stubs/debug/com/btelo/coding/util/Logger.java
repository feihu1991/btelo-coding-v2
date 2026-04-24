package com.btelo.coding.util;

import android.util.Log;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一的日志记录工具
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\"\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010\u0003\n\u0002\b\b\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\"\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u00042\u0006\u0010\u000e\u001a\u00020\u00042\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\u0010J\u000e\u0010\u0011\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u0004J\"\u0010\u0012\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u00042\u0006\u0010\u000e\u001a\u00020\u00042\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\u0010J\u000e\u0010\u0013\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u0004J\"\u0010\u0014\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u00042\u0006\u0010\u000e\u001a\u00020\u00042\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\u0010J\u0006\u0010\u0015\u001a\u00020\fJ\u0010\u0010\u0016\u001a\u00020\n2\u0006\u0010\r\u001a\u00020\u0004H\u0002J\"\u0010\u0017\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u00042\u0006\u0010\u000e\u001a\u00020\u00042\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\u0010R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00040\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000RN\u0010\u0007\u001aB\u0012\f\u0012\n \t*\u0004\u0018\u00010\u00040\u0004\u0012\f\u0012\n \t*\u0004\u0018\u00010\n0\n \t* \u0012\f\u0012\n \t*\u0004\u0018\u00010\u00040\u0004\u0012\f\u0012\n \t*\u0004\u0018\u00010\n0\n\u0018\u00010\b0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0018"}, d2 = {"Lcom/btelo/coding/util/Logger;", "", "()V", "TAG_PREFIX", "", "debugTags", "", "enabledTags", "Ljava/util/concurrent/ConcurrentHashMap$KeySetView;", "kotlin.jvm.PlatformType", "", "d", "", "tag", "message", "throwable", "", "disableTag", "e", "enableTag", "i", "init", "isEnabled", "w", "app_debug"})
public final class Logger {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG_PREFIX = "BTELO_";
    private static final java.util.concurrent.ConcurrentHashMap.KeySetView<java.lang.String, java.lang.Boolean> enabledTags = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.util.Set<java.lang.String> debugTags = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.btelo.coding.util.Logger INSTANCE = null;
    
    private Logger() {
        super();
    }
    
    public final void init() {
    }
    
    public final void d(@org.jetbrains.annotations.NotNull()
    java.lang.String tag, @org.jetbrains.annotations.NotNull()
    java.lang.String message, @org.jetbrains.annotations.Nullable()
    java.lang.Throwable throwable) {
    }
    
    public final void i(@org.jetbrains.annotations.NotNull()
    java.lang.String tag, @org.jetbrains.annotations.NotNull()
    java.lang.String message, @org.jetbrains.annotations.Nullable()
    java.lang.Throwable throwable) {
    }
    
    public final void w(@org.jetbrains.annotations.NotNull()
    java.lang.String tag, @org.jetbrains.annotations.NotNull()
    java.lang.String message, @org.jetbrains.annotations.Nullable()
    java.lang.Throwable throwable) {
    }
    
    public final void e(@org.jetbrains.annotations.NotNull()
    java.lang.String tag, @org.jetbrains.annotations.NotNull()
    java.lang.String message, @org.jetbrains.annotations.Nullable()
    java.lang.Throwable throwable) {
    }
    
    private final boolean isEnabled(java.lang.String tag) {
        return false;
    }
    
    public final void enableTag(@org.jetbrains.annotations.NotNull()
    java.lang.String tag) {
    }
    
    public final void disableTag(@org.jetbrains.annotations.NotNull()
    java.lang.String tag) {
    }
}