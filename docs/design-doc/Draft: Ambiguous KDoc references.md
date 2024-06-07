#  Ambiguous KDoc references

In KDoc there are two types of references to declaration:
-   Fully qualified ones;
-   Relative ones;

Also, KDoc allows to refer to:
-   Parameters
-   A receiver via [this]
    
Relative references can be ambiguous that means there more than one possible candidate.
For trivial cases (ambiguous references are inside only one scope) there are the defined priorities of KDoc candidates in the Dokka and IDE K1 implementations:  
-   class
-   package
-   function
-   property

For example,
```kotlin
val x = 0
fun x() = 0

/** here [x] refers to the function */
```

# Problem

For relative references there is a problem of determining in which scopes a search of  a refereed declaration should be done to avoid ambiguous references. (see https://kotlinlang.org/spec/scopes-and-identifiers.html#scopes-and-identifiers)

### I Order of scopes

Let's consider the the following example with a possible ambiguous references:
```kotlin
class B

/** [B] */
class A {
	class B // K1, Javadoc and K2 refer to a nested class B.
}
```
K1 (IDE and Dokka), K2, and Javadoc refer to a nested class B. Then let's change the example a little:

```kotlin
open class C {
	class B
}
class B

/**
* [B] K2 and Javadoc lead to [C.B], but K1 leads to the outer B
*/
class A : C()
```
This causes inconsistent behavior. In K1, the reference `[B]` lead to the outer B.
*Note: This document does not consider the case of documentation for extensions.  It can deserve another dedicated document.*

Also,  unlike KDoc, Javadoc takes visibility into account.
```java
public class JavaC {}
public class JavaB {  
    private class JavaC {}  
}
/**  
 * {@link JavaC} to the outer JavaC
 * since JavaB.JavaC is private
 */
 public class JavaA extends JavaB {}
```


### II Reference to itself
Javadoc and KDoc allow to have references to itself. It is a quite spread practice. 
However it can also lead to ambiguous reference:
```kotlin
 /**
 *  [A] In K1, it leads to the nested class A.A. In K2 - to the outer class A
 */
class A {
	class A
}
```
The case (a nested class with the same name as enclosing class) can be unpopular since it is banned in Java, C#.

There is a more practical case with a factory function :
```kotlin
/** [A] */
class A
/** [A] */
fun A(p: Int) = A()
```
In K1, the both reference lead to the class A. In K2, they lead to itself.
This case can be applied to all possible pairs of declaration kinds (function and function, property and class...).

Also, a constructor has a name of class.
```kotlin
class A{
	/** 
	* [A] In K1, it leads to the class A. In K2 - to the constructor 
	*/
	constructor(s: String)
}
```
### III References to parameters

In a primary constructor, the same reference can refer to parameters and properties. It does not matter for IDE. Opposite,  Dokka has different locations for parameters and properties.
```kotlin
/**
* [abc] K1 refers to the property `abc`, K2 - to the parameter
*/
class A(val abc: String)
```
From the point of IDE view, the link `[abc]` leads to the same position in a source file independently of whether the `abc` is a parameter or property.

#### Availability/Visibility of parameters
The availability of parameters inside a scope can result in ambiguous references.
```kotlin
class A(a: Int) {  
	/**  
	* [a] is unresolved in K1. In K2 it is resolved
	*/  
  fun usage() = 0
}
```


## Proposals
This section consider different solutions. It is unnecessary to choose only one for all cases above.
Choosing  a solution can be related to the future support of  KDoc references to overloads and a possible process of deprecation of the old KDoc links.
Whether  resolving KDoc references should take visibility into account is an open question.

### Universal solution
Currently, the Analysis API returns a list of KDoc candidates. 
Dokka can show all possible candidates like IDE does it for ambiguous resolving.
**Pros:**
* it seems suitable for overloads
**Cons:** 
* might be difficult to implement in HTML. It requires a drop-down list.
* possible irrelevant candidates, for example
	```kotlin
	fun A = 0
	class A
	/**
	The class [A]..., but it also shows the irrelevant fun
	*/
	```

### Per case solution
The solutions is based on creating a set of rules for each case.

#### Case I

Currently, an inner scope of classifier already has a priority over outer scopes.  
By the Kotlin specification, a scope is a syntactically-delimited region. 
 1.  Consider a member scope (including inherited members and classes) ignoring the visibility
 Pros:
 - a majority of cases is consistent  with Javadoc
 - corresponds to the rule of thumb for KDoc references:
*Links should behave as if they were written in code.*  *(The exception is visibility)*
 Cons:
 ??
```kotlin
open class C {
	class B
}
class B
/**
* [B] refers to [C.B]
*/
class A : C()
```
 2.  TODO
 
#### Case II
1. Reference to itself should hide other available declarations.
```kotlin
/**
* [A] should refer to itself
*/
class A {
	/**
	* [A] should refer to itself
	*/
	class A
	/**
	* [A] should refer to itself
	* to have a reference to a class, the fully qualified reference [package.A.A] should be used
	*/
	fun A(a: Int)
}
```
2. The behavior of K1 may seem counterintuitive for users.

#### Case III
1. Parameters of current declaration should hide other declaration.  The visibility of parameters inside a class body is still questionable.
```kotlin
/**
* [abc] should refer to the parameter
* [A.abc] should refer to the property
*/
class A(val abc: String)
```

2. Otherwise, we have no possibility to have a reference to a parameter from a documentation of class since having a reference to parameters in a documentation of class can have no sense.
However, such a reference should be allowed in a dog tag section `@constructor`
```kotlin
/**
* [abc] and [A.abc] should refer to the property
* @constructor [abc] should refer to the parameter
*/
class A(val abc: String) {
// here no access to parameters
}
```



## Other languages

-     
    [https://docs.oracle.com/javase/specs/jls/se8/html/jls-8.html#jls-8.1](https://docs.oracle.com/javase/specs/jls/se8/html/jls-8.html#jls-8.1)
    
    It is a compile-time error if a class has the same simple name as any of its enclosing classes or interfaces.
    

### JavaScript (JSDoc)

For `@link` tag ( https://jsdoc.app/tags-inline-link ) there is a namepath. A namepath provides a way to do so and disambiguate between instance members, static members and inner variables. [https://jsdoc.app/about-namepaths](https://jsdoc.app/about-namepaths)

JSDoc does not have such a problem since it has a unique identifier like a fully qualified path in Kotlin.

  

### Python (Sphinx)

The Python allows to have a cross-reference via the markup see [https://www.sphinx-doc.org/en/master/usage/domains/python.html#cross-referencing-python-objects](https://www.sphinx-doc.org/en/master/usage/domains/python.html#cross-referencing-python-objects)  
There are some roles:  `:py:class` `:py:func`  `:py:meth:` `:py:attr:` and so on.

> Normally, names in these roles are searched first without any further qualification, then with the current module name prepended, then with the current module and class name (if any) prepended.

> If you prefix the name with a dot, this order is reversed. For example, in the documentation of Python’s [codecs](https://docs.python.org/3/library/codecs.html#module-codecs) module, :py:func:`open` always refers to the built-in function, while :py:func:`.open` refers to [codecs.open()](https://docs.python.org/3/library/codecs.html#codecs.open).

> Also, if the name is prefixed with a dot, and no exact match is found, the target is taken as a suffix and all object names with that suffix are searched. For example, :py:meth:`.TarFile.close` references the tarfile.TarFile.close() function, even if the current module is not tarfile. Since this can get ambiguous, if there is more than one possible match, you will get a warning from Sphinx.

### Swift
TODO

### C#

It has XML documentation [https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/documentation-comments](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/documentation-comments)  
  
The `cref` attribute is used to provide a reference to a code element

The documentation generator must respect namespace visibility according to using statements appearing within the source code.

Examples:  
`cref="System.Security.PermissionSet"`
and relative references:
```csharp
public class Point {
/// <summary>
/// This method changes the point's location to
/// the given coordinates. <see cref="Translate"/>
/// </summary>
public void Move(int xPosition, int yPosition) {
...
}

/// <summary>This method changes the point's location by
/// the given x- and y-offsets. <see cref="Move"/>
/// </summary>
public void Translate(int dx, int dy) {
...
}
}
```

As Java, C# does not allow to have a nested class with the same name as enclosing class

Other languages (e.g. Rust, Golang..) do not have the concept of nested classes.
