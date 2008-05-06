package ee.ut.f2f.core.mpi.message;

import java.lang.reflect.Method;

public class BasicMessage {

	public String toString() {
		StringBuffer content = new StringBuffer(getClass().getName());
		Method[] metodo = this.getClass().getDeclaredMethods();
		for (int i = 0; i < metodo.length; i++) {
			if (!metodo[i].getName().equals("clone") && !metodo[i].getName().equals("toString") && metodo[i].getParameterTypes().length == 0) {
				try {
					content.append(" ").append(metodo[i].getName()).append("=").append(metodo[i].invoke(this, new Object[] {}));
				} catch (Exception e) {
				}
			}
		}
		return content.toString();
	}
}
