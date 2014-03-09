MacroGroovy [![Build Status](https://travis-ci.org/bsideup/MacroGroovy.png)](https://travis-ci.org/bsideup/MacroGroovy)
===

Tired of Groovy ast transformation generation code? 

```groovy
def someVariable = new ConstantExpression("someValue");
def returnStatement = new ReturnStatement(
    new ConstructorCallExpression(
        ClassHelper.make(SomeCoolClass),
        new ArgumentListExpression(someVariable)
    )
);
```

Looks familar, huh? Maybe this will be better?
```groovy
def someVariable = macro { "someValue" };
ReturnStatement returnStatement = macro { return new SomeCoolClass($v{ someVariable }) }
```

Now it's possible with MacroGroovy!

Why it's better than AstBuilder?
----------------

1.  MacroGroovy is easy to use, just call method named "macro" (it's available to all Objects as Extension Method)
2.  MacroGroovy doesn't require you to create some AstBuilder instances
3.  MacroGroovy supports substitution ($v inside code), which is impossible with AstBuilder
4.  MacroGroovy returns result of exactly the same type as expression, no ugly casting at all
5.  MacroGroovy rely on AstBuilder code, so it's tested by core Groovy team and safe to use!
6.  You can even use macro inside macro:

```groovy
def constructorCall = macro { new SomeCoolClass($v{ macro { "someValue" } }) }
```

![Build Status](http://i1.kym-cdn.com/photos/images/newsfeed/000/001/123/xzibit-wtf.jpg)]

Installation
---------

*   Add it to your project:

```xml
<dependency>
    <groupId>ru.trylogic.groovy.macro</groupId>
    <artifactId>macro-groovy</artifactId>
    <version>1.1.1</version>
</dependency>
```
*   Watch how fast your code becomes easy-to-read and lighter
*   ???
*   PROFIT!


License
-----------------

Copyright (c) 2013 Sergei bsideup Egorov

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
documentation files (the "Software"), to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial
portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
