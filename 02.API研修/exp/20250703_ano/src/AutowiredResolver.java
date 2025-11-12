import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public class AutowiredResolver {
    public static void checkMaxLength(Object object) {
        Constructor<? extends Object> constructors = object.getClass().getDeclaredConstructor();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            MaxLength fieldAnno = field.getAnnotation(MaxLength.class);
            if (fieldAnno == null)
                continue; // 【2】
            try {
                field.setAccessible(true);
                String str = (String) field.get(object); // 【3】
                int max = fieldAnno.value(); // 【4】
                if (max < str.length()) { // 【5】
                    throw new RuntimeException(
                            "文字列長が指定された長さを超えています"); // 【6】
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
