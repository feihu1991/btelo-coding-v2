package com.btelo.coding.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.btelo.coding.data.local.entity.SessionEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SessionDao_Impl implements SessionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SessionEntity> __insertionAdapterOfSessionEntity;

  private final EntityDeletionOrUpdateAdapter<SessionEntity> __deletionAdapterOfSessionEntity;

  private final EntityDeletionOrUpdateAdapter<SessionEntity> __updateAdapterOfSessionEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteSessionById;

  private final SharedSQLiteStatement __preparedStmtOfUpdateConnectionStatus;

  private final SharedSQLiteStatement __preparedStmtOfUpdateLastActiveTime;

  public SessionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSessionEntity = new EntityInsertionAdapter<SessionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `sessions` (`id`,`name`,`tool`,`createdAt`,`lastActiveAt`,`isConnected`,`currentKeyVersion`,`lastKeyRotation`,`rotationIntervalDays`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SessionEntity entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getId());
        }
        if (entity.getName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getName());
        }
        if (entity.getTool() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getTool());
        }
        statement.bindLong(4, entity.getCreatedAt());
        statement.bindLong(5, entity.getLastActiveAt());
        final int _tmp = entity.isConnected() ? 1 : 0;
        statement.bindLong(6, _tmp);
        statement.bindLong(7, entity.getCurrentKeyVersion());
        statement.bindLong(8, entity.getLastKeyRotation());
        statement.bindLong(9, entity.getRotationIntervalDays());
      }
    };
    this.__deletionAdapterOfSessionEntity = new EntityDeletionOrUpdateAdapter<SessionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `sessions` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SessionEntity entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getId());
        }
      }
    };
    this.__updateAdapterOfSessionEntity = new EntityDeletionOrUpdateAdapter<SessionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `sessions` SET `id` = ?,`name` = ?,`tool` = ?,`createdAt` = ?,`lastActiveAt` = ?,`isConnected` = ?,`currentKeyVersion` = ?,`lastKeyRotation` = ?,`rotationIntervalDays` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SessionEntity entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getId());
        }
        if (entity.getName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getName());
        }
        if (entity.getTool() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getTool());
        }
        statement.bindLong(4, entity.getCreatedAt());
        statement.bindLong(5, entity.getLastActiveAt());
        final int _tmp = entity.isConnected() ? 1 : 0;
        statement.bindLong(6, _tmp);
        statement.bindLong(7, entity.getCurrentKeyVersion());
        statement.bindLong(8, entity.getLastKeyRotation());
        statement.bindLong(9, entity.getRotationIntervalDays());
        if (entity.getId() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getId());
        }
      }
    };
    this.__preparedStmtOfDeleteSessionById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM sessions WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateConnectionStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE sessions SET isConnected = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateLastActiveTime = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE sessions SET lastActiveAt = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertSession(final SessionEntity session,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSessionEntity.insert(session);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteSession(final SessionEntity session,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfSessionEntity.handle(session);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateSession(final SessionEntity session,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfSessionEntity.handle(session);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteSessionById(final String sessionId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteSessionById.acquire();
        int _argIndex = 1;
        if (sessionId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, sessionId);
        }
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteSessionById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateConnectionStatus(final String sessionId, final boolean isConnected,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateConnectionStatus.acquire();
        int _argIndex = 1;
        final int _tmp = isConnected ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        if (sessionId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, sessionId);
        }
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateConnectionStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateLastActiveTime(final String sessionId, final long timestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateLastActiveTime.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 2;
        if (sessionId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, sessionId);
        }
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateLastActiveTime.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<SessionEntity>> getAllSessions() {
    final String _sql = "SELECT * FROM sessions ORDER BY lastActiveAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"sessions"}, new Callable<List<SessionEntity>>() {
      @Override
      @NonNull
      public List<SessionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfTool = CursorUtil.getColumnIndexOrThrow(_cursor, "tool");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastActiveAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastActiveAt");
          final int _cursorIndexOfIsConnected = CursorUtil.getColumnIndexOrThrow(_cursor, "isConnected");
          final int _cursorIndexOfCurrentKeyVersion = CursorUtil.getColumnIndexOrThrow(_cursor, "currentKeyVersion");
          final int _cursorIndexOfLastKeyRotation = CursorUtil.getColumnIndexOrThrow(_cursor, "lastKeyRotation");
          final int _cursorIndexOfRotationIntervalDays = CursorUtil.getColumnIndexOrThrow(_cursor, "rotationIntervalDays");
          final List<SessionEntity> _result = new ArrayList<SessionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SessionEntity _item;
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final String _tmpTool;
            if (_cursor.isNull(_cursorIndexOfTool)) {
              _tmpTool = null;
            } else {
              _tmpTool = _cursor.getString(_cursorIndexOfTool);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpLastActiveAt;
            _tmpLastActiveAt = _cursor.getLong(_cursorIndexOfLastActiveAt);
            final boolean _tmpIsConnected;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsConnected);
            _tmpIsConnected = _tmp != 0;
            final int _tmpCurrentKeyVersion;
            _tmpCurrentKeyVersion = _cursor.getInt(_cursorIndexOfCurrentKeyVersion);
            final long _tmpLastKeyRotation;
            _tmpLastKeyRotation = _cursor.getLong(_cursorIndexOfLastKeyRotation);
            final int _tmpRotationIntervalDays;
            _tmpRotationIntervalDays = _cursor.getInt(_cursorIndexOfRotationIntervalDays);
            _item = new SessionEntity(_tmpId,_tmpName,_tmpTool,_tmpCreatedAt,_tmpLastActiveAt,_tmpIsConnected,_tmpCurrentKeyVersion,_tmpLastKeyRotation,_tmpRotationIntervalDays);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<SessionEntity> getSessionById(final String sessionId) {
    final String _sql = "SELECT * FROM sessions WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (sessionId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, sessionId);
    }
    return CoroutinesRoom.createFlow(__db, false, new String[] {"sessions"}, new Callable<SessionEntity>() {
      @Override
      @Nullable
      public SessionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfTool = CursorUtil.getColumnIndexOrThrow(_cursor, "tool");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastActiveAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastActiveAt");
          final int _cursorIndexOfIsConnected = CursorUtil.getColumnIndexOrThrow(_cursor, "isConnected");
          final int _cursorIndexOfCurrentKeyVersion = CursorUtil.getColumnIndexOrThrow(_cursor, "currentKeyVersion");
          final int _cursorIndexOfLastKeyRotation = CursorUtil.getColumnIndexOrThrow(_cursor, "lastKeyRotation");
          final int _cursorIndexOfRotationIntervalDays = CursorUtil.getColumnIndexOrThrow(_cursor, "rotationIntervalDays");
          final SessionEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final String _tmpTool;
            if (_cursor.isNull(_cursorIndexOfTool)) {
              _tmpTool = null;
            } else {
              _tmpTool = _cursor.getString(_cursorIndexOfTool);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpLastActiveAt;
            _tmpLastActiveAt = _cursor.getLong(_cursorIndexOfLastActiveAt);
            final boolean _tmpIsConnected;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsConnected);
            _tmpIsConnected = _tmp != 0;
            final int _tmpCurrentKeyVersion;
            _tmpCurrentKeyVersion = _cursor.getInt(_cursorIndexOfCurrentKeyVersion);
            final long _tmpLastKeyRotation;
            _tmpLastKeyRotation = _cursor.getLong(_cursorIndexOfLastKeyRotation);
            final int _tmpRotationIntervalDays;
            _tmpRotationIntervalDays = _cursor.getInt(_cursorIndexOfRotationIntervalDays);
            _result = new SessionEntity(_tmpId,_tmpName,_tmpTool,_tmpCreatedAt,_tmpLastActiveAt,_tmpIsConnected,_tmpCurrentKeyVersion,_tmpLastKeyRotation,_tmpRotationIntervalDays);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getSessionByIdSync(final String sessionId,
      final Continuation<? super SessionEntity> $completion) {
    final String _sql = "SELECT * FROM sessions WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (sessionId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, sessionId);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SessionEntity>() {
      @Override
      @Nullable
      public SessionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfTool = CursorUtil.getColumnIndexOrThrow(_cursor, "tool");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastActiveAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastActiveAt");
          final int _cursorIndexOfIsConnected = CursorUtil.getColumnIndexOrThrow(_cursor, "isConnected");
          final int _cursorIndexOfCurrentKeyVersion = CursorUtil.getColumnIndexOrThrow(_cursor, "currentKeyVersion");
          final int _cursorIndexOfLastKeyRotation = CursorUtil.getColumnIndexOrThrow(_cursor, "lastKeyRotation");
          final int _cursorIndexOfRotationIntervalDays = CursorUtil.getColumnIndexOrThrow(_cursor, "rotationIntervalDays");
          final SessionEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final String _tmpTool;
            if (_cursor.isNull(_cursorIndexOfTool)) {
              _tmpTool = null;
            } else {
              _tmpTool = _cursor.getString(_cursorIndexOfTool);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpLastActiveAt;
            _tmpLastActiveAt = _cursor.getLong(_cursorIndexOfLastActiveAt);
            final boolean _tmpIsConnected;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsConnected);
            _tmpIsConnected = _tmp != 0;
            final int _tmpCurrentKeyVersion;
            _tmpCurrentKeyVersion = _cursor.getInt(_cursorIndexOfCurrentKeyVersion);
            final long _tmpLastKeyRotation;
            _tmpLastKeyRotation = _cursor.getLong(_cursorIndexOfLastKeyRotation);
            final int _tmpRotationIntervalDays;
            _tmpRotationIntervalDays = _cursor.getInt(_cursorIndexOfRotationIntervalDays);
            _result = new SessionEntity(_tmpId,_tmpName,_tmpTool,_tmpCreatedAt,_tmpLastActiveAt,_tmpIsConnected,_tmpCurrentKeyVersion,_tmpLastKeyRotation,_tmpRotationIntervalDays);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
