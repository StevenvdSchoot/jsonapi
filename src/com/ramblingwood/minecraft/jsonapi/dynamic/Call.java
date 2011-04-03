package com.ramblingwood.minecraft.jsonapi.dynamic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.bukkit.Server;

import com.ramblingwood.minecraft.jsonapi.JSONAPI;

public class Call {
	private static Server Server = JSONAPI.instance.getServer();
	private static APIWrapperMethods APIInstance = APIWrapperMethods.getInstance();
	
	private Class<?>[] signature;
	private ArrayList<Object> stack = new ArrayList<Object>();
	private HashMap<Integer, String> defaults = new HashMap<Integer, String>();
	private boolean debug = false;

	public Call (String input, ArgumentList args) {
		parseString(input);
		signature = args.getTypes();
	}
	
	private void debug(String in) {
		if(debug) {
			System.out.println(in);
		}
	}
	
	public Object call(Object[] params) throws Exception {
		int size = stack.size();
		
		ArrayList<Object> oParams = new ArrayList<Object>(Arrays.asList(params));
		
		for(Integer i : defaults.keySet()) {
			oParams.add(i, defaults.get(i));
		}
		
		debug("oParams:"+oParams.toString());
		debug("Stack:"+stack);
		
		Object lastResult = null;
		for(int i = 0; i < size; i++) {
			Object v = stack.get(i);
			debug("v:"+v.getClass().getCanonicalName());
			if(v instanceof Server || v instanceof APIWrapperMethods) {
				lastResult = v;
				continue;
			}
			if(v instanceof SubCall) {
				SubCall obj = (SubCall)v;
				
				debug("Calling method: '"+obj.getName()+"' with signature: '"+obj.requiresArgs()+"' '"+Arrays.asList(sigForIndices(obj.requiresArgs()))+"'.");
				debug("Last result:"+lastResult.toString());
				debug("Invoking method: '"+obj.getName()+"' with args: '"+Arrays.asList(indicies(oParams, obj.requiresArgs()))+"'.");
				
				lastResult = lastResult.getClass().getMethod(obj.getName(), sigForIndices(obj.requiresArgs())).invoke(lastResult, indicies(oParams, obj.requiresArgs()));
				
				debug("New value:"+lastResult);
			}
		}
		
		return lastResult;
	}
	
	public Object[] indicies (ArrayList<Object> o, ArrayList<Integer>i) {
		if(i == null) { return null; }
		
		Object[] ret = new Object[i.size()];
		for(int y = 0; y < ret.length; y++) {
			ret[y] = o.get(i.get(y));
		}
		
		return ret;
	}
	
	public Class<?>[] sigForIndices (ArrayList<Integer> i) {
		if(i == null) { return null; }
		
		Class<?>[] ret = new Class<?>[i.size()];
		for(int y = 0; y < ret.length; y++) {
			ret[y] = signature[i.get(y)];
		}
		
		return ret;
	}
	
	public void parseString (String input) {
		String[] parts = input.split("\\.");

		for(int i = 0; i < parts.length; i++) {
			String v = parts[i];
			//if(i == 0) {
				if(v.equals("Server")) {
					stack.add(Server);
				}
				else if(v.equals("this")) {
					stack.add(APIInstance);
				}
			//}
			else {
				// no args
				if(v.endsWith("()") || !v.endsWith(")")) {
					stack.add(new SubCall(v.substring(0, v.length()-2), new ArrayList<Integer>()));
				}
				
				// args
				else {
					// find the position of the args
					int startPos = v.lastIndexOf("(");
					
					// take the stuff in the ('s and )'s 
					String[] argParts = v.substring(startPos+1, v.length() - 1).split(",");

					ArrayList<Integer> argPos = new ArrayList<Integer>();
					
					// put all string 0, 1, 2, 3 etc into a int[]
					int multiplier = 0;
					
					for(int x = 0; x < argParts.length; x++) {
						if(argParts[x].startsWith("\"") && argParts[x].endsWith("\"")) {
							defaults.put(x, v.substring(1, v.length() - 1));
							
							multiplier++;
						}
						else {
							argPos.add(Integer.parseInt(argParts[x].trim()) + (multiplier));
						}
					}
					
					// add this "method" onto the stack
					stack.add(new SubCall(v.substring(0, startPos), argPos));
				}
			}
		}
	}
	
	class SubCall {
		private String name;
		private ArrayList<Integer> argPos;
		
		public SubCall(String name, ArrayList<Integer> argPos) {
			this.name = name;
			this.argPos = argPos;
		}
		
		public ArrayList<Integer> requiresArgs () {
			return (argPos.size() > 0 ? argPos : null);
		}
		
		public String getName() {
			return name;
		}
	}
	
	public Object callMethod(String method, String[] signature, Object[] params) throws Exception {
		String[] parts = method.split("\\.");

		Class<?>[] ps = new Class<?>[signature.length];
		for(int i = 0; i< signature.length; i++) {
			try {
				ps[i] = Class.forName(signature[i]);
			}
			catch(ClassNotFoundException e) {
				ps[i] = Class.forName("java.lang."+signature[i]);
			}
		}

		Class c = Class.forName(parts[0]);
		Object lastResult = new Object();
		for(int i = 0; i < parts.length; i++) {
			if(i == 0) {
				lastResult = c.getMethod(parts[i+1], null).invoke(null, null);
			}
			else if(i == (parts.length - 1)) {
				return lastResult;
			}
			else {
				lastResult = lastResult.getClass().getMethod(parts[i+1], ps).invoke(lastResult, params);
			}
		}

		return lastResult;
	}	
}