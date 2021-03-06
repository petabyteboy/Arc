package arc.ecs.weaver.impl;


import arc.ecs.weaver.*;
import arc.ecs.weaver.meta.*;
import org.objectweb.asm.*;

import static arc.ecs.weaver.meta.ClassMetadataUtil.superName;

class CommonClassWeaver extends ClassVisitor implements Opcodes{

    private ClassMetadata meta;

    public CommonClassWeaver(ClassVisitor cv, ClassMetadata meta){
        super(ASM4, cv);
        this.meta = meta;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces){
        cv.visit(version, access, name, signature, superName(meta), interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible){
        return (Weaver.POOLED_ANNOTATION.equals(desc))
        ? null
        : super.visitAnnotation(desc, visible);
    }
}
