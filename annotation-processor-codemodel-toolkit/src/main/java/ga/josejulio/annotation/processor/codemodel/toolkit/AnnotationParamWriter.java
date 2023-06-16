package ga.josejulio.annotation.processor.codemodel.toolkit;

import com.sun.codemodel.JAnnotationArrayMember;
import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JType;

class AnnotationParamWriter {

    private final String key;
    private final JAnnotationUse annotationUse;
    private final JAnnotationArrayMember annotationArrayMember;

    public AnnotationParamWriter(String key, JAnnotationUse annotationUse) {
        this.key = key;
        this.annotationUse = annotationUse;
        this.annotationArrayMember = null;
    }

    private AnnotationParamWriter(String key, JAnnotationArrayMember annotationArrayMember) {
        this.key = key;
        this.annotationUse = null;
        this.annotationArrayMember = annotationArrayMember;
    }

    public AnnotationParamWriter writeArray() {
        if (annotationUse != null) {
            return new AnnotationParamWriter(key, annotationUse.paramArray(key));
        }

        throw new IllegalStateException("Write array is only allowed when annotationUse is not null. annotationUse is null");
    }

    public void writeNumber(Number number) {
        if (number instanceof Integer) {
            writeInteger(number.intValue());
        } else if (number instanceof Float) {
            writeFloat(number.floatValue());
        } else if (number instanceof Byte) {
            writeByte(number.byteValue());
        } else if (number instanceof Double) {
            writeDouble(number.doubleValue());
        } else if (number instanceof Long) {
            writeLong(number.longValue());
        } else if (number instanceof Short) {
            writeShort(number.shortValue());
        } else {
            throw new RuntimeException("Unexpected number type: " + number);
        }
    }

    public void writeString(String value) {
        if (annotationUse != null) {
            annotationUse.param(key, value);
        } else {
            annotationArrayMember.param(value);
        }
    }

    public void writeJType(JType value) {
        if (annotationUse != null) {
            annotationUse.param(key, value);
        } else {
            annotationArrayMember.annotate((JClass) value);
        }
    }

    public JAnnotationUse writeClass(Class<? extends java.lang.annotation.Annotation> value) {
        if (annotationUse != null) {
            return annotationUse.annotationParam(key, value);
        } else {
            return annotationArrayMember.annotate(value);
        }
    }

    public void writeEnum(Enum<?> value) {
        if (annotationUse != null) {
            annotationUse.param(key, value);
        } else {
            annotationArrayMember.param(value);
        }
    }

    public void writeInteger(int value) {
        if (annotationUse != null) {
            annotationUse.param(key, value);
        } else {
            annotationArrayMember.param(value);
        }
    }

    public void writeLong(long value) {
        if (annotationUse != null) {
            annotationUse.param(key, value);
        } else {
            annotationArrayMember.param(value);
        }
    }

    public void writeShort(short value) {
        if (annotationUse != null) {
            annotationUse.param(key, value);
        } else {
            annotationArrayMember.param(value);
        }
    }

    public void writeByte(byte value) {
        if (annotationUse != null) {
            annotationUse.param(key, value);
        } else {
            annotationArrayMember.param(value);
        }
    }

    public void writeFloat(float value) {
        if (annotationUse != null) {
            annotationUse.param(key, value);
        } else {
            annotationArrayMember.param(value);
        }
    }

    public void writeDouble(double value) {
        if (annotationUse != null) {
            annotationUse.param(key, value);
        } else {
            annotationArrayMember.param(value);
        }
    }
}
