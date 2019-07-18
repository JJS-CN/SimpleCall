package com.simplelibrary.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 添加注解在activity上，在activity跳转时，将对权限进行判断，用户授权拒绝时将不进行页面跳转
 * Created by felear on 2018/4/25.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NeedPermission {
    String[] value();

    String msgDenied() default "";//异常时提示用户的提示语
}
