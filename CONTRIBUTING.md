# Functional contribution

We are welcome for any contribution. But every new feature implemented in this plugin should:
 
- Be useful enough for lot of people (should not cover only your professional case)
- Should not break existing use cases and should avoid breaking the backward compatibility in existing APIs.
  - If the compatibility break is required, it should be well justified. 
    [Guide](https://wiki.eclipse.org/Evolving_Java-based_APIs_2) 
    and [jenkins solutions](https://wiki.jenkins-ci.org/display/JENKINS/Hint+on+retaining+backward+compatibility) can help to retain the backward compatibility   
- Should be easily maintained (so maintainers need some time to think about architecture of implementation)
- Have at least one test for positive use case

This plugin is used by lot of people, so it should be stable enough. Please ensure your change is compatible at least with the last LTS line. 
Any core dependency upgrade must be justified

# Code Style Guidelines

Most of rules is checked with help of the *maven-checkstyle-plugin* during the `validate` phase. 
Checkstyle rules are more important than this document.

## Resulting from long experience

* To the largest extent possible, all fields shall be private. Use an IDE to generate the getters and setters.
* If a class has more than one `volatile` member field, it is probable that there are subtle race conditions. Please consider where appropriate encapsulation of the multiple fields into an immutable value object replace the multiple `volatile` member fields with a single `volatile` reference to the value object (or perhaps better yet an `AtomicReference` to allow for `compareAndSet` - if compare-and-set logic is appropriate).
* If it is `Serializable` it shall have a `serialVersionUID` field. Unless code has shipped to users, the initial value of the `serialVersionUID` field shall be `1L`.

## Indentation

1. **Use spaces.** Tabs are banned.
2. **Java blocks are 4 spaces.** JavaScript blocks as for Java. **XML nesting is 4 spaces**

## Field Naming Conventions

1. "hungarian"-style notation is banned (i.e. instance variable names preceded by an 'm', etc)
2. If the field is `static final` then it shall be named in `ALL_CAPS_WITH_UNDERSCORES`.
3. Start variable names with a lowercase letter and use camelCase rather than under_scores.
4. Spelling and abreviations: If the word is widely used in the JVM runtime, stick with the spelling/abreviation in the JVM runtime, e.g. `color` over `colour`, `sync` over `synch`, `async` over `asynch`, etc.
5. It is acceptable to use `i`, `j`, `k` for loop indices and iterators. If you need more than three, you are likely doing something wrong and as such you shall either use full descriptive names or refactor.
6. It is acceptable to use `e` for the exception in a `try...catch` block.
7. You shall never use `l` (i.e. lower case `L`) as a variable name.

## Line Length

To the greatest extent possible, please wrap lines to ensure that they do not exceed 120 characters.

## Maven POM file layout

* The `pom.xml` file shall use the sequencing of elements as defined by the `mvn tidy:pom` command (after any indenting fix-up).
* If you are introducing a property to the `pom.xml` the property must be used in at least two distinct places in the model or a comment justifying the use of a property shall be provided.
* If the `<plugin>` is in the groupId `org.apache.maven.plugins` you shall omit the `<groupId>`.
* All `<plugin>` entries shall have an explicit version defined unless inherited from the parent.

## Java code style

### Imports

* For code in `src/main`:
    - `*` imports are banned. 
    - `static` imports are preferred until not mislead.
* For code in `src/test`:
    - `*` imports of anything other than JUnit classes and Hamcrest matchers are banned.

### Annotation placement

* Annotations on classes, interfaces, annotations, enums, methods, fields and local variables shall be on the lines immediately preceding the line where modifier(s) (e.g. `public` / `protected` / `private` / `final`, etc) would be appropriate.
* Annotations on method arguments shall, to the largest extent possible, be on the same line as the method argument (and, if present, before the `final` modifier)

### Javadoc

* Each class shall have a Javadoc comment.
* Unless the method is `private`, it shall have a Javadoc comment.
* Getters and Setters shall have a Javadoc comment. The following is prefered
    ```
    /**
     * The count of widgets
     */
    private int widgetCount;
    
    /**
     * Returns the count of widgets.
     *
     * @return the count of widgets. 
     */
    public int getWidgetCount() {
        return widgetCount;
    }
    
    /**
     * Sets the count of widgets.
     *
     * @param widgetCount the count of widgets.
     */
    public void setWidgetCount(int widgetCount) {
        this.widgetCount = widgetCount;
    }
    ```
* When adding a new class / interface / etc, it shall have a `@since` doc comment. The version shall be `FIXME` (or `TODO`) to indicate that the person merging the change should replace the `FIXME` with the next release version number. The fields and methods within a class/interface (but not nested classes) will be assumed to have the `@since` annotation of their class/interface unless a different `@since` annotation is present.

### IDE Configuration

* Eclipse, by and large the IDE defaults are acceptable with the following changes:
    - Tab policy to `Spaces only`
    - Indent statements within `switch` body
    - Maximum line width `120`
    - Line wrapping, ensure all to `wrap where necessary`
    - Organize imports alphabetically, no grouping
* NetBeans, by and large the IDE defaults are acceptable with the following changes:
    - Tabs and Indents
        + Change Right Margin to `120`
        + Indent case statements in switch
    - Wrapping
        + Change all the `Never` values to `If Long`
        + Select the checkbox for Wrap After Assignement Operators
* IntelliJ, by and large the IDE defaults are acceptable with the following changes:
    - Wrapping and Braces
        + Change `Do not wrap` to `Wrap if long`
        + Change `Do not force` to `Always`
    - Javadoc
        + Disable generating `<p/>` on empty lines
    - Imports
        + Class count to use import with '*': `9999`
        + Names count to use static import with '*': `99999`
        + Import Layout
            * import all other imports
            * blank line
            * import static all other imports
    
## Issues

This project uses [Jenkins Jira issue tracker](https://issues.jenkins-ci.org) 
with [github-plugin](https://issues.jenkins-ci.org/browse/JENKINS/component/15896) component.
            
## Links 

- https://wiki.jenkins-ci.org/display/JENKINS/contributing
- https://wiki.jenkins-ci.org/display/JENKINS/Extend+Jenkins
