package android.net.networkstack.util;

import android.util.Log;
import android.util.SparseArray;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class MessageUtils {
    public static final String[] DEFAULT_PREFIXES = {"CMD_", "EVENT_"};
    private static final String TAG = "MessageUtils";

    public static class DuplicateConstantError extends Error {
        public DuplicateConstantError(String str, String str2, int i) {
            super(String.format("Duplicate constant value: both %s and %s = %d", str, str2, Integer.valueOf(i)));
        }
    }

    public static SparseArray<String> findMessageNames(Class[] clsArr, String[] strArr) {
        boolean isFinal;
        SparseArray<String> sparseArray = new SparseArray<>();
        for (Class cls : clsArr) {
            String name = cls.getName();
            try {
                Field[] declaredFields = cls.getDeclaredFields();
                for (Field field : declaredFields) {
                    int modifiers = field.getModifiers();
                    if (!(Modifier.isFinal(modifiers) ^ true) && !(!Modifier.isStatic(modifiers))) {
                        String name2 = field.getName();
                        for (String str : strArr) {
                            if (name2.startsWith(str)) {
                                field.setAccessible(true);
                                try {
                                    int i = field.getInt(null);
                                    try {
                                        String str2 = sparseArray.get(i);
                                        if (str2 != null) {
                                            if (!str2.equals(name2)) {
                                                throw new DuplicateConstantError(name2, str2, i);
                                            }
                                        }
                                        sparseArray.put(i, name2);
                                    } catch (IllegalAccessException | SecurityException unused) {
                                        continue;
                                    }
                                } catch (ExceptionInInitializerError | IllegalArgumentException unused2) {
                                    continue;
                                }
                            }
                        }
                        continue;
                    }
                }
                continue;
            } catch (SecurityException unused3) {
                Log.e(TAG, "Can't list fields of class " + name);
            }
        }
        return sparseArray;
    }

    public static SparseArray<String> findMessageNames(Class[] clsArr) {
        return findMessageNames(clsArr, DEFAULT_PREFIXES);
    }
}
