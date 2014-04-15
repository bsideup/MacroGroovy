package ru.trylogic.groovy.macro;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

public class MacroExtensionMethods {
    
    public static class ValuePlaceholder {
        public static Object $v(Closure cl) {
            return null; // replaced with AST transformations
        }
    }
    
    public static <T> T macro(Object self, @DelegatesTo(ValuePlaceholder.class) Closure cl) {
        return null;
    }

    public static <T> T macro(Object self, boolean asIs, @DelegatesTo(ValuePlaceholder.class) Closure cl) {
        return null;
    }
}
