package droidsafe.utils;

import droidsafe.android.app.Project;

import droidsafe.main.Config;

import droidsafe.speclang.ArgumentValue;
import droidsafe.speclang.Method;
import droidsafe.speclang.TypeValue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.lang.ClassNotFoundException;
import java.lang.RuntimeException;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.AnySubType;

import soot.ArrayType;

import soot.BooleanType;

import soot.ByteType;

import soot.CharType;

import soot.DoubleType;

import soot.FloatType;

import soot.Hierarchy;

import soot.IntType;

import soot.jimple.ClassConstant;
import soot.jimple.DoubleConstant;
import soot.jimple.FloatConstant;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.JasminClass;
import soot.jimple.Jimple;
import soot.jimple.LongConstant;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.StmtBody;
import soot.jimple.StringConstant;

import soot.LongType;

import soot.NullType;

import soot.PrimType;

import soot.Printer;

import soot.RefLikeType;

import soot.RefType;

import soot.Scene;

import soot.ShortType;

import soot.SootClass;

import soot.SootMethod;

import soot.SootMethodRef;

import soot.tagkit.LineNumberTag;

import soot.Type;

import soot.Unit;

import soot.util.Chain;
import soot.util.JasminOutputStream;

import soot.Value;

import soot.VoidType;

/**
 * Class to hold general utility methods that are helpful for Soot.
 * 
 * @author mgordon
 *
 */
public class SootUtils {
	private final static Logger logger = LoggerFactory.getLogger(SootUtils.class);
	
	public static final Pattern sigRE = Pattern.compile("<(\\S+): (\\S+) (\\S+)\\((.*)\\)>");

  /*
   * Takes in a Soot Value and if it's a constant, returns a java object with the same value
   */
  public static Object constantValueToObject(Value sootValue) throws ClassNotFoundException {
    if(sootValue instanceof NullConstant) {
      return null;
    } else if (sootValue instanceof IntConstant) {
      return new Integer(((IntConstant)sootValue).value);
    } else if (sootValue instanceof StringConstant) {
      System.out.println("Value/Box");
      System.out.println(sootValue);
      System.out.println(sootValue.getUseBoxes());
      String string = new String(((StringConstant)sootValue).value);
      droidsafe.model.java.lang.String droidsafeString = new droidsafe.model.java.lang.String();
      droidsafeString.incorporateString(string);
      return droidsafeString;
    } else if (sootValue instanceof LongConstant) {
      return new Long(((LongConstant)sootValue).value);
    } else if (sootValue instanceof DoubleConstant) {
      return new Double(((DoubleConstant)sootValue).value);
    } else if (sootValue instanceof FloatConstant) {
      return new Float(((FloatConstant)sootValue).value);
    } else if (sootValue instanceof ClassConstant) {
      String className = ((ClassConstant)sootValue).value.replace("/", ".");
      try {
        return Class.forName(className);
      } catch(ClassNotFoundException cnfe) {
        // TODO: Ficure out how to get iusntances of app-specific classes
        String string = new String("class" + className);
        droidsafe.model.java.lang.String droidsafeString = new droidsafe.model.java.lang.String();
        droidsafeString.incorporateString(string);
        return droidsafeString;
      }
    }
    
    throw new RuntimeException("Unhandled java primitive sootValue: " + sootValue);
  }

	/*
	 * Given a string representing a type in soot, (ex: int, java.lang.Class[]), return 
	 * the appropriate Soot type for the object. 
	 */
	public static Type toSootType(String str) {
		if (str.equals("int"))
			return IntType.v();
		else if (str.equals("char"))
			return CharType.v();
		else if (str.equals("boolean"))
			return BooleanType.v();
		else if (str.equals("long"))
			return LongType.v();
		else if (str.equals("byte"))
			return ByteType.v();
		else if (str.equals("double"))
			return DoubleType.v();
		else if (str.equals("float"))
			return FloatType.v();
		else if (str.equals("short"))
			return ShortType.v();
		else if (str.equals("void")) 
			return VoidType.v();
		else if (str.equals("null"))
			return NullType.v();
		else {
			//not a primitive type
			Pattern typeSig = Pattern.compile("([\\w\\$\\.]+)([\\[\\]]*)");
			Matcher matcher = typeSig.matcher(str);
			boolean b = matcher.matches();
			if (!b || matcher.groupCount() != 2) {
				Utils.ERROR_AND_EXIT(logger, "Something very wrong with parsing type: {}", str);
			}
			
			String baseType = matcher.group(1);
			
			
			if (!matcher.group(2).equals("")) {
				//array type
				String brackets = matcher.group(2);
				int numDims = brackets.length() / 2;
				return ArrayType.v(toSootType(baseType), numDims);
			} else if (!matcher.group(1).equals("")) {
				//class type
				return RefType.v(baseType);
			} else {
				Utils.ERROR_AND_EXIT(logger, "Cannot parse type: {}", str);
				return null;
			}
		}
	}
	
	public static boolean isStringType(Type t) {
		return t.equals(RefType.v("java.lang.String"));
	}
	  
	public static boolean isClassType(Type t) {
		return t.equals(RefType.v("java.lang.Class"));
	}
	
    /**
     * Return true if type is a integral type (byte, char, short, int, or long).
     */
    public static boolean isIntegral(Type t) {
    	return (t instanceof LongType ||
    		t instanceof IntType ||
    		t instanceof ShortType ||
    		t instanceof CharType ||
    		t instanceof ByteType); 
    }
    
    /**
     * Return a BF traversal of the super interfaces of a class.
     */
    public static List<SootClass> getSuperInterfacesOf(SootClass sc) {
    	 List<SootClass>   ret = new LinkedList<SootClass>();
         Queue<SootClass> q   = new LinkedList<SootClass>();
         //add initial interfaces
         for (SootClass i : sc.getInterfaces()) {
        	 q.add(i);
         }

         while (!q.isEmpty()) {
             SootClass curr = q.poll();
                          
             if (curr == null) {
            	 continue;
             }
             
             if (curr.toString().equals("java.lang.Object")) {
                 continue;
             }
             
             if (!curr.isInterface()) {
            	 logger.error("getSuperInterfacesOf inspecting non interface: {}", curr);
            	 System.exit(1);
             }
                    
             ret.add(curr);
             
             if (!curr.isPhantom() && curr.getSuperclass().isInterface()) {
             	q.add(curr.getSuperclass());
             }
           
         }

         return ret;
     }
         
    /**
     * Get all superclasses and super interfaces of a soot class.
     */
    public static Set<SootClass> getParents(SootClass sc) {
        Set<SootClass>   ret = new LinkedHashSet<SootClass>();
        Queue<SootClass> q   = new LinkedList<SootClass>();
        q.add(sc);

        while (!q.isEmpty()) {
            SootClass curr = q.poll();
            if (curr == null) {
            	logger.info("Strange, null found in getParents on {}", sc);
            	continue;
            }
            
            if (curr.toString().equals("java.lang.Object")) {
                continue;
            }
            
            if (!curr.isPhantom()){ 
            	q.addAll(curr.getInterfaces());
            	ret.addAll(curr.getInterfaces());
            }
            
            if (curr.hasSuperclass() && !curr.isPhantom()) {
            	q.add(curr.getSuperclass());
            	ret.add(curr.getSuperclass());
            }
        }

        return ret;
    }
    
    /**
     * Given a class or interface, get all classes that have this as ancestor.
     */
    public static List<SootClass> getChildrenIncluding(SootClass sc) {
    	Hierarchy hier = Scene.v().getActiveHierarchy();
        
    	if (sc.isInterface()) {
    		LinkedList<SootClass> classes = new LinkedList<SootClass>();
    		//want all sub interfaces, and all implementing classes
            classes.addAll(hier.getSubinterfacesOfIncluding(sc));
            classes.addAll(hier.getImplementersOf(sc));
            classes.add(sc);
            return classes;
    	}
    	else 
    		return hier.getSubclassesOfIncluding(sc);
    		
    }
    
    /**
     * Return true if child is a subtype of parent.  If both are primitive integral types, 
     * return true.If both are arraytypes, make sure element type of child is subtype of element 
     * type of parent.
     */
    public static boolean isSubTypeOfIncluding(Type child, Type parent) {
    	if (child.equals(parent))
    		return true;
    	else if (child instanceof NullType ||
    			parent instanceof NullType)
    		return true;
    	else if (parent instanceof PrimType) {
    		//special case the integral types because they can be cast to
    		//each other and the spec language only have integer types
    		if (isIntegral(parent) && isIntegral(child))
    			return true;
    		return parent.equals(child);
    	} else if (parent instanceof ArrayType && child instanceof ArrayType) 	{
    		return isSubTypeOfIncluding(((ArrayType)child).getElementType(), ((ArrayType)parent).getElementType());
    	} else if (parent instanceof RefType && child instanceof RefType) {
    		SootClass pClass = ((RefType)parent).getSootClass();
    		SootClass cClass = ((RefType)child).getSootClass();
    		
    		Set<SootClass> parents = getParents(cClass);
    		return parents.contains(pClass);
    		
    	} else if (parent instanceof VoidType && child instanceof VoidType) {
    		return true;
    	} else if (parent instanceof NullType && child instanceof NullType) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    /**
     * Given the signature of a method that may or may not concretely exist, search 
     * for the concrete call that will be resolved for the signature.
     * 
     * This search entails a polymorphic search over all the methods of the class
     * for return types and parameter types.  
     * 
     * It then searches parent classes if the method cannot be found in this class.
     */
    public static SootMethod resolveMethod(SootClass clz, String signature) {
    	if (Scene.v().containsMethod(signature)) 
    		return Scene.v().getMethod(signature);
    	
    	//check this class for the method with polymorpism
    	String mName = grabName(signature);
    	String[] args = grabArgs(signature);
    	String rtype = grabReturnType(signature);
    	
    	for (SootMethod curr : clz.getMethods()) {
    		if (!curr.getName().equals(mName) || curr.getParameterCount() != args.length)
    			continue;
    		
    		//check the return types
    		Type returnType = toSootType(rtype);
    		if (!isSubTypeOfIncluding(returnType, curr.getReturnType())) 
    			continue;
    		
    		for (int i = 0; i < args.length; i++) 
    			if (!isSubTypeOfIncluding(toSootType(args[i]), curr.getParameterType(i)))
    				continue;
    		
    		//if we got here all is well and we found a method that matches!
    		return curr;
    	}
    	
    	//if not found, and at object can't find it
    	if (clz.getName().equals("java.lang.Object") ||
    			clz.getSuperclass() == null)
    		return null;
    	
    	//now check the parents
    	return resolveMethod(clz.getSuperclass(), signature);
    }
   
    /**
     * Grab the args string from the method signature 
     */
    public static String[] grabArgs(String signature) {
    	Matcher matcher = sigRE.matcher(signature);
		boolean b = matcher.matches();
		
		if (!b && matcher.groupCount() != 4)
			logger.error("Bad method signature: {}", signature);
		
		//args, create the args string array
		String args = matcher.group(4);
		
		if (args.isEmpty())
			return new String[0];
		
		return args.split(",");		
    }
    
    /**
     * Grab the class from the signature
     */
    public static String grabClass(String signature) {
		Matcher matcher = sigRE.matcher(signature);
		boolean b = matcher.matches();
		
		if (!b && matcher.groupCount() != 4)
			logger.error("Bad method signature: {}", signature);
		
		return matcher.group(1);
    }
    
    /**
     * Grab the method name from the signature
     */
    public static String grabName(String signature) {
		Matcher matcher = sigRE.matcher(signature);
		boolean b = matcher.matches();
		
		if (!b && matcher.groupCount() != 4)
			logger.error("Bad method signature: {}", signature);
		
		return matcher.group(3);
    }
    
    /**
     * Grab the method return type string from the signature
     */
    public static String grabReturnType(String signature) {
    	Matcher matcher = sigRE.matcher(signature);
		boolean b = matcher.matches();
		
		if (!b && matcher.groupCount() != 4)
			Utils.ERROR_AND_EXIT(logger,"Cannot create Method from DroidBlaze Signature");
		
		return matcher.group(2);
		
    }
    
    /**
     * Given a class, method name, return type string (in soot format) and the number of args,
     * try to find the method in the class, plus any methods that it could override in superclasses
     * and interfaces.
     */
    public static List<SootMethod> findPossibleInheritedMethods(SootClass clz, String name, String returnType, int numArgs) {
    	Hierarchy hierarchy = Scene.v().getActiveHierarchy();
    	LinkedList<SootMethod> methods = new LinkedList<SootMethod>();
    	
    	List<SootClass> classes = new LinkedList<SootClass>();
    	
    	if (!clz.isInterface()) 
    		classes.addAll(hierarchy.getSuperclassesOfIncluding(clz));
    	
    	classes.addAll(clz.getInterfaces());
   
    	
    	for (SootClass parent : classes) {
    		for (SootMethod method : parent.getMethods()) {
    			logger.debug("Looking at {} in {}", method, parent);
    			if (method.getName().equals(name) &&
    					method.getParameterCount() == numArgs &&
    					method.getReturnType().toString().equals(returnType))
    				methods.add(method);
    		}
    	}
    	
    	return methods;
    }
    
    
    /**
     * Load classes from the given jar file into Soot's current scene.  
     * Load the classes as application classes if appClass is true.
     * If overwrite is true, then overwrite any classes that were previously loaded.  If 
     * overwrite is false, then don't load from this jar any previously loaded classes.
     * 
     * Return a set of all classes loaded from the jar.
     */
    public static Set<SootClass> loadClassesFromJar(JarFile jarFile, boolean appClass, Set<String> doNotLoad) {
    	LinkedHashSet<SootClass> classSet = new LinkedHashSet<SootClass>();
        Enumeration allEntries = jarFile.entries();
        while (allEntries.hasMoreElements()) {
            JarEntry entry = (JarEntry) allEntries.nextElement();
            String   name  = entry.getName();
            if (!name.endsWith(".class")) {
                continue;
            }

            String clsName = name.substring(0, name.length() - 6).replace('/', '.');
            
            if (doNotLoad.contains(clsName)) {
            	continue;
            }
            
            if (appClass) {
            	//SootClass clz = Scene.v().loadClassAndSupport(clsName);
            	SootClass clz = Scene.v().loadClass(clsName, SootClass.BODIES);
            	classSet.add(clz);
            	clz.setApplicationClass();
            	logger.debug("Loading from {}: {} (app)", jarFile.getName(), clsName);
            }
            else {
            	SootClass clz = Scene.v().loadClass(clsName, SootClass.SIGNATURES);
            	classSet.add(clz);
            	clz.setLibraryClass();
            	logger.debug("Loading from {}: {} (lib)", jarFile.getName(), clsName);
            }

        }
        return classSet;
    }
    
    /**
     * For the given class, return a constructor with the fewest number of 
     * arguments.
     */
    public static SootMethod findSimpliestConstructor(SootClass clz) {
    	SootMethod currentCons = null;
    	int currentConsArgs = Integer.MAX_VALUE;
    	
    	for (SootMethod method : clz.getMethods()) {
    		if (method.isConstructor() && method.getParameterCount() < currentConsArgs) {
    			currentCons = method;
    			currentConsArgs = method.getParameterCount();
    		}
    	}
    	
    	return currentCons;
    }
	
    /**
     * Given a type, return a valid value for that type.
     */
    public static Value getNullValue(Type type) {
    	if (type instanceof BooleanType)
    		return IntConstant.v(1);
    	else if (type instanceof IntType)
    		return IntConstant.v(0);
    	else if (type instanceof LongType)
    		return LongConstant.v(0);
    	else if (type instanceof FloatType)
    		return FloatConstant.v((float) 0.0);
    	else if (type instanceof DoubleType) 
    		return DoubleConstant.v(0.0);
    	else if (type instanceof CharType) 	
    		return IntConstant.v(48); 
    	else {
    		return NullConstant.v();
    	}
    }
    
    /**
     * Return a concrete implementor of interface or abstract class.
     */
    public static SootClass getCloseConcrete(SootClass clz) {
    	if (clz.isInterface()) 
    		return getCloseImplementor(clz);
    	else if (clz.isAbstract()) 
    		return getCloseSubclass(clz);
    	else 
    		return null;
    }
    
    /**
     * Return a concrete implementor of a given interface.  Try to find direct implementors first
     */
    public static SootClass getCloseSubclass(SootClass clz) {
    	if (!clz.isAbstract() && !clz.isInterface())
    		Utils.ERROR_AND_EXIT(logger, "Trying to get close subclass of a non abstract class: {}", clz);
    	
    	logger.debug("Trying to get direct subclasses for: {}", clz);
    	//try to get direct implementors by adding them first
    	List<SootClass> implementors = 
    			new LinkedList<SootClass>();
    	
    	implementors.addAll((List<SootClass>)Scene.v().getActiveHierarchy().getDirectSubclassesOf(clz));  
    	
    	implementors.addAll(Scene.v().getActiveHierarchy().getSubclassesOf(clz));
    
    	//return the first concrete class
    	for (SootClass c : implementors) 
    		if (c.isConcrete())
    			return c;

    	return null;
    }
    
    /**
     * Return a concrete implementor of a given interface.  Try to find direct implementors first
     */
    public static SootClass getCloseImplementor(SootClass clz) {
    	if (!clz.isInterface())
    		Utils.ERROR_AND_EXIT(logger, "Trying to get implementor of a non interface: {}", clz);
    	
    	//try to get direct implementors by adding them first
    	List<SootClass> implementors = new LinkedList<SootClass>();
    			
    	implementors.addAll((List<SootClass>)Scene.v().getActiveHierarchy().getDirectImplementersOf(clz));  

    	implementors.addAll(Scene.v().getActiveHierarchy().getImplementersOf(clz));
    
    	//return the first concrete class
    	for (SootClass c : implementors) 
    		if (c.isConcrete())
    			return c;

    	return null;
    }
    
    /**
	 * Write the class and jimple file in the output directory. Prefix is the absolute 
	 * file name prefix for the class and jimple files (.class and .jimple will be appended).
	 */
	public static void writeByteCodeAndJimple(String filePrefix, SootClass clz) {
		String fileName = filePrefix + ".class";
        String methodThatFailed = "";
		try {
			OutputStream streamOut = new JasminOutputStream(
					new FileOutputStream(fileName));
			PrintWriter writerOut = new PrintWriter(
					new OutputStreamWriter(streamOut));
			
			for (SootMethod method : clz.getMethods()) {
                methodThatFailed = method.getName();
				if (method.isConcrete())
					method.retrieveActiveBody();
			}
			
			/*
			JasminClass jasminClass = new soot.jimple.JasminClass(clz);
		    jasminClass.print(writerOut);
		    writerOut.flush();
		    streamOut.close();
		    */
		    fileName = filePrefix + ".jimple";
		    streamOut = new FileOutputStream(fileName);
		    writerOut = new PrintWriter(
		                                new OutputStreamWriter(streamOut));
		    Printer.v().printTo(clz, writerOut);
		    writerOut.flush();
		    streamOut.close();
			
		} catch (Exception e) {
            logger.info("Method that failed = " + methodThatFailed);
			logger.info("Error writing class to file {}", clz, e);
		}
	}
	
	/**
	 * Return the source location of a method based on its first statement.
	 */
	public static SourceLocationTag getMethodLocation(SootMethod method) {
		if (method.isConcrete()) {
			Chain<Unit> stmts = ((StmtBody)method.retrieveActiveBody()).getUnits();
			Iterator stmtIt = stmts.snapshotIterator();
			
			if (stmtIt.hasNext()) {
				return getSourceLocation((Stmt)stmtIt.next(), method.getDeclaringClass());
			}		
		}
		
		return null;
	}

	public static SourceLocationTag getSourceLocation(Stmt stmt, SootClass clz) {
		 SourceLocationTag line = null;
		 
		 if (stmt == null || clz == null) {
			 return line;
		 }
         
		 LineNumberTag tag = (LineNumberTag) stmt.getTag("LineNumberTag");
		 if (tag != null) {
			 line = new SourceLocationTag(clz.toString(),  tag.getLineNumber());
		 }
		 
		 return line;
	}
	
	
	
	/**
	 * Wrapped resolve concrete dispatch method that will throw a more useful exception when
	 * it cannot find the method.
	 */
	public static SootMethod resolveConcreteDispatch(SootClass clz, SootMethod meth) 
			throws CannotFindMethodException {
		try {
			return Scene.v().getActiveHierarchy().resolveConcreteDispatch( clz, meth);
		} catch (Exception e) {
			throw new CannotFindMethodException(clz, meth);
		}
	}
	
	public static Type getBaseType(RefLikeType type) {
		if (type instanceof ArrayType) 
			return ((ArrayType)type).getArrayElementType();
		else if (type instanceof AnySubType) 
			return ((AnySubType)type).getBase();
		else 
			return type;
	}
	
	/**
	 * Try to grab an instance invoke expr from a statement, if it does not
	 * have one, return null.
	 */
	public static InstanceInvokeExpr getInstanceInvokeExpr(Stmt stmt) {
		if (!stmt.containsInvokeExpr()) 
			return null;
		
		InvokeExpr expr = (InvokeExpr)stmt.getInvokeExpr();
		
		if (!(expr instanceof InstanceInvokeExpr)) 
			return null;
		
		return (InstanceInvokeExpr)expr;
	}
	
	public static Set<SootMethod> getOverridingMethodsIncluding(SootClass clz, String subSig) {
		Set<SootMethod> methods = new LinkedHashSet<SootMethod>();
		
		List<SootClass> childrenIncluding = getChildrenIncluding(clz);
				
		for (SootClass child : childrenIncluding) {
			if (child.declaresMethod(subSig)) {
				SootMethod meth = child.getMethod(subSig);
				if (!meth.isAbstract())
					methods.add(meth);
			}
		}
		
		return methods;
	}
	
	/**
	 * Given two classes that could be related (one is a parent of the other or vice versa), return 
	 * the child class.  If not related, then return null
	 */
	public static SootClass narrowerClass(SootClass c1, SootClass c2) {
		Hierarchy hier = Scene.v().getActiveHierarchy();
		
		if (c1.isInterface() && c2.isInterface()) {
			logger.error("Cannot find a narrower concrete class for {} and {}", c1, c2);
			System.exit(1);
		}
			
		if (c1.isInterface())
			return c2;
		
		if (c2.isInterface())
			return c1;
		
		if (!hier.isClassSuperclassOfIncluding(c1, c2) &&
				!hier.isClassSuperclassOf(c2, c1)) {
			return null;
		}
		
		return hier.isClassSuperclassOfIncluding(c1, c2) ? c2 : c1;
	}
	
	/**
	 * Starting at a class, return all the set of all concrete classes that implement the class,
	 * stop the search at each concrete class at each branch.  So if we pass in a concrete class, just return it.
	 */
	public static List<SootClass> smallestConcreteSetofImplementors(SootClass clz) {
		List<SootClass> clazzes = new LinkedList<SootClass>();
		Hierarchy h = Scene.v().getActiveHierarchy();
		
		if (clz.isConcrete()) {
			clazzes.add(clz);
			return clazzes;
		} 
		
		List<SootClass> imps;
		
		if (clz.isAbstract() && !clz.isInterface()) {
			imps = h.getDirectSubclassesOf(clz);
		} else if (clz.isInterface()) {
			imps = new LinkedList<SootClass>();
			imps.addAll(h.getImplementersOf(clz));
			imps.addAll(h.getSubinterfacesOf(clz));
		} else {
			return clazzes;
		}
		
		for (SootClass imp : imps) {
			
			if (imp.isConcrete())
				clazzes.add(imp);
			else
				clazzes.addAll(smallestConcreteSetofImplementors(imp));
		}
		
		return clazzes;
	}
	
}


