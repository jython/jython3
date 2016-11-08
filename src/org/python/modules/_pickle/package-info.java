package org.python.modules._pickle;
/**
 *
 * From the python documentation:
 * <p>
 * The <tt>PickleModule.java</tt> module implements a basic but powerful algorithm
 * for ``pickling'' (a.k.a. serializing, marshalling or flattening) nearly
 * arbitrary Python objects.  This is the act of converting objects to a
 * stream of bytes (and back: ``unpickling'').
 * This is a more primitive notion than
 * persistency -- although <tt>PickleModule.java</tt> reads and writes file
 * objects, it does not handle the issue of naming persistent objects, nor
 * the (even more complicated) area of concurrent access to persistent
 * objects.  The <tt>PickleModule.java</tt> module can transform a complex object
 * into a byte stream and it can transform the byte stream into an object
 * with the same internal structure.  The most obvious thing to do with these
 * byte streams is to write them onto a file, but it is also conceivable
 * to send them across a network or store them in a database.  The module
 * <tt>shelve</tt> provides a simple interface to pickle and unpickle
 * objects on ``dbm''-style database files.
 * <P>
 * <b>Note:</b> The <tt>PickleModule.java</tt> have the same interface as the
 * standard module <tt>pickle</tt>except that <tt>PyPickler</tt> and
 * <tt>Unpickler</tt> are factory functions, not classes (so they cannot be
 * used as base classes for inheritance).
 * This limitation is similar for the original PickleModule.c version.
 *
 * <P>
 * Unlike the built-in module <tt>marshal</tt>, <tt>PickleModule.java</tt> handles
 * the following correctly:
 * <P>
 *
 * <UL><LI>recursive objects (objects containing references to themselves)
 *
 * <P>
 *
 * <LI>object sharing (references to the same object in different places)
 *
 * <P>
 *
 * <LI>user-defined classes and their instances
 *
 * <P>
 *
 * </UL>
 *
 * <P>
 * The data format used by <tt>PickleModule.java</tt> is Python-specific.  This has
 * the advantage that there are no restrictions imposed by external
 * standards such as XDR (which can't represent pointer sharing); however
 * it means that non-Python programs may not be able to reconstruct
 * pickled Python objects.
 *
 * <P>
 * By default, the <tt>PickleModule.java</tt> data format uses a printable ASCII
 * representation.  This is slightly more voluminous than a binary
 * representation.  The big advantage of using printable ASCII (and of
 * some other characteristics of <tt>PickleModule.java</tt>'s representation) is
 * that for debugging or recovery purposes it is possible for a human to read
 * the pickled file with a standard text editor.
 *
 * <P>
 * A binary format, which is slightly more efficient, can be chosen by
 * specifying a nonzero (true) value for the <i>bin</i> argument to the
 * <tt>PyPickler</tt> constructor or the <tt>dump()</tt> and <tt>dumps()</tt>
 * functions.  The binary format is not the default because of backwards
 * compatibility with the Python 1.4 pickle module.  In a future version,
 * the default may change to binary.
 *
 * <P>
 * The <tt>PickleModule.java</tt> module doesn't handle code objects.
 * <P>
 * For the benefit of persistency modules written using <tt>PickleModule.java</tt>,
 * it supports the notion of a reference to an object outside the pickled
 * data stream.  Such objects are referenced by a name, which is an
 * arbitrary string of printable ASCII characters.  The resolution of
 * such names is not defined by the <tt>PickleModule.java</tt> module -- the
 * persistent object module will have to implement a method
 * <tt>persistent_load()</tt>.  To write references to persistent objects,
 * the persistent module must define a method <tt>persistent_id()</tt> which
 * returns either <tt>None</tt> or the persistent ID of the object.
 *
 * <P>
 * There are some restrictions on the pickling of class instances.
 *
 * <P>
 * First of all, the class must be defined at the top level in a module.
 * Furthermore, all its instance variables must be picklable.
 *
 * <P>
 *
 * <P>
 * When a pickled class instance is unpickled, its <tt>__init__()</tt> method
 * is normally <i>not</i> invoked.  <b>Note:</b> This is a deviation
 * from previous versions of this module; the change was introduced in
 * Python 1.5b2.  The reason for the change is that in many cases it is
 * desirable to have a constructor that requires arguments; it is a
 * (minor) nuisance to have to provide a <tt>__getinitargs__()</tt> method.
 *
 * <P>
 * If it is desirable that the <tt>__init__()</tt> method be called on
 * unpickling, a class can define a method <tt>__getinitargs__()</tt>,
 * which should return a <i>tuple</i> containing the arguments to be
 * passed to the class constructor (<tt>__init__()</tt>).  This method is
 * called at pickle time; the tuple it returns is incorporated in the
 * pickle for the instance.
 * <P>
 * Classes can further influence how their instances are pickled -- if the
 * class defines the method <tt>__getstate__()</tt>, it is called and the
 * return state is pickled as the contents for the instance, and if the class
 * defines the method <tt>__setstate__()</tt>, it is called with the
 * unpickled state.  (Note that these methods can also be used to
 * implement copying class instances.)  If there is no
 * <tt>__getstate__()</tt> method, the instance's <tt>__dict__</tt> is
 * pickled.  If there is no <tt>__setstate__()</tt> method, the pickled
 * object must be a dictionary and its items are assigned to the new
 * instance's dictionary.  (If a class defines both <tt>__getstate__()</tt>
 * and <tt>__setstate__()</tt>, the state object needn't be a dictionary
 * -- these methods can do what they want.)  This protocol is also used
 * by the shallow and deep copying operations defined in the <tt>copy</tt>
 * module.
 * <P>
 * Note that when class instances are pickled, their class's code and
 * data are not pickled along with them.  Only the instance data are
 * pickled.  This is done on purpose, so you can fix bugs in a class or
 * add methods and still load objects that were created with an earlier
 * version of the class.  If you plan to have long-lived objects that
 * will see many versions of a class, it may be worthwhile to put a version
 * number in the objects so that suitable conversions can be made by the
 * class's <tt>__setstate__()</tt> method.
 *
 * <P>
 * When a class itself is pickled, only its name is pickled -- the class
 * definition is not pickled, but re-imported by the unpickling process.
 * Therefore, the restriction that the class must be defined at the top
 * level in a module applies to pickled classes as well.
 *
 * <P>
 *
 * <P>
 * The interface can be summarized as follows.
 *
 * <P>
 * To pickle an object <tt>x</tt> onto a file <tt>f</tt>, open for writing:
 *
 * <P>
 * <dl><dd><pre>
 * p = pickle.PyPickler(f)
 * p.dump(x)
 * </pre></dl>
 *
 * <P>
 * A shorthand for this is:
 *
 * <P>
 * <dl><dd><pre>
 * pickle.dump(x, f)
 * </pre></dl>
 *
 * <P>
 * To unpickle an object <tt>x</tt> from a file <tt>f</tt>, open for reading:
 *
 * <P>
 * <dl><dd><pre>
 * u = pickle.Unpickler(f)
 * x = u.load()
 * </pre></dl>
 *
 * <P>
 * A shorthand is:
 *
 * <P>
 * <dl><dd><pre>
 * x = pickle.load(f)
 * </pre></dl>
 *
 * <P>
 * The <tt>PyPickler</tt> class only calls the method <tt>f.write()</tt> with a
 * string argument.  The <tt>Unpickler</tt> calls the methods
 * <tt>f.read()</tt> (with an integer argument) and <tt>f.readline()</tt>
 * (without argument), both returning a string.  It is explicitly allowed to
 * pass non-file objects here, as long as they have the right methods.
 *
 * <P>
 * The constructor for the <tt>PyPickler</tt> class has an optional second
 * argument, <i>bin</i>.  If this is present and nonzero, the binary
 * pickle format is used; if it is zero or absent, the (less efficient,
 * but backwards compatible) text pickle format is used.  The
 * <tt>Unpickler</tt> class does not have an argument to distinguish
 * between binary and text pickle formats; it accepts either format.
 *
 * <P>
 * The following types can be pickled:
 *
 * <UL><LI><tt>None</tt>
 *
 * <P>
 *
 * <LI>integers, long integers, floating point numbers
 *
 * <P>
 *
 * <LI>strings
 *
 * <P>
 *
 * <LI>tuples, lists and dictionaries containing only picklable objects
 *
 * <P>
 *
 * <LI>classes that are defined at the top level in a module
 *
 * <P>
 *
 * <LI>instances of such classes whose <tt>__dict__</tt> or
 * <tt>__setstate__()</tt> is picklable
 *
 * <P>
 *
 * </UL>
 *
 * <P>
 * Attempts to pickle unpicklable objects will raise the
 * <tt>PicklingError</tt> exception; when this happens, an unspecified
 * number of bytes may have been written to the file.
 *
 * <P>
 * It is possible to make multiple calls to the <tt>dump()</tt> method of
 * the same <tt>PyPickler</tt> instance.  These must then be matched to the
 * same number of calls to the <tt>load()</tt> method of the
 * corresponding <tt>Unpickler</tt> instance.  If the same object is
 * pickled by multiple <tt>dump()</tt> calls, the <tt>load()</tt> will all
 * yield references to the same object.  <i>Warning</i>: this is intended
 * for pickling multiple objects without intervening modifications to the
 * objects or their parts.  If you modify an object and then pickle it
 * again using the same <tt>PyPickler</tt> instance, the object is not
 * pickled again -- a reference to it is pickled and the
 * <tt>Unpickler</tt> will return the old value, not the modified one.
 * (There are two problems here: (a) detecting changes, and (b)
 * marshalling a minimal set of changes.  I have no answers.  Garbage
 * Collection may also become a problem here.)
 *
 * <P>
 * Apart from the <tt>PyPickler</tt> and <tt>Unpickler</tt> classes, the
 * module defines the following functions, and an exception:
 *
 * <P>
 * <dl><dt><b><tt>dump</tt></a></b> (<var>object, file</var><big>[</big><var>,
 *      bin</var><big>]</big>)
 * <dd>
 * Write a pickled representation of <i>obect</i> to the open file object
 * <i>file</i>.  This is equivalent to
 * "<tt>PyPickler(<i>file</i>, <i>bin</i>).dump(<i>object</i>)</tt>".
 * If the optional <i>bin</i> argument is present and nonzero, the binary
 * pickle format is used; if it is zero or absent, the (less efficient)
 * text pickle format is used.
 * </dl>
 *
 * <P>
 * <dl><dt><b><tt>load</tt></a></b> (<var>file</var>)
 * <dd>
 * Read a pickled object from the open file object <i>file</i>.  This is
 * equivalent to "<tt>Unpickler(<i>file</i>).load()</tt>".
 * </dl>
 *
 * <P>
 * <dl><dt><b><tt>dumps</tt></a></b> (<var>object</var><big>[</big><var>,
 *     bin</var><big>]</big>)
 * <dd>
 * Return the pickled representation of the object as a string, instead
 * of writing it to a file.  If the optional <i>bin</i> argument is
 * present and nonzero, the binary pickle format is used; if it is zero
 * or absent, the (less efficient) text pickle format is used.
 * </dl>
 *
 * <P>
 * <dl><dt><b><tt>loads</tt></a></b> (<var>string</var>)
 * <dd>
 * Read a pickled object from a string instead of a file.  Characters in
 * the string past the pickled object's representation are ignored.
 * </dl>
 *
 * <P>
 * <dl><dt><b><a name="l2h-3763"><tt>PicklingError</tt></a></b>
 * <dd>
 * This exception is raised when an unpicklable object is passed to
 * <tt>PyPickler.dump()</tt>.
 * </dl>
 *
 *
 * <p>
 * For the complete documentation on the pickle module, please see the
 * "Python Library Reference"
 * <p><hr><p>
 *
 * The module is based on both original pickle.py and the PickleModule.c
 * version, except that all mistakes and errors are my own.
 * <p>
 * @author Finn Bock, bckfnn@pipmail.dknet.dk
 * @version PickleModule.java,v 1.30 1999/05/15 17:40:12 fb Exp
 */