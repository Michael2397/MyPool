import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedList;

import sun.font.CreatedFontTracker;

/**
 * 
 */

/**
 * @author michael
 * Description: 
 *代理：如果对某个接口中的某个指定的方法的功能进行扩展，而不想实现接口里所有方法，可以使用（动态代理）代理模式
 *java中代理模式:静态/动态/cglib（Spring）
 *使用动态代理，可以检测接口中方法的执行
 *
 *Proxy
 *static Object newProxyInstance(
 *ClassLoader loader,  定义代理类的类加载器
 *Class<?>[] interfaces,   代理类要实现的接口列表
 *InvocationHandler h)  指派方法调用的调用处理程序 
 * 2017年5月1日
 */
public class Pool {
	private int init_count = 3;
	private int max_count = 6;
	private int current_count = 0;
	private LinkedList<Connection> pool = new LinkedList<>();
	
	//1.构造函数，初始化连接放入连接池
	public Pool(){
		for (int i = 0; i < init_count; i++) {
			current_count++;
			Connection conn = createConnection();
			pool.add(conn);
		}
	}
	
	//2.创建一个新的连接方法
	private Connection createConnection(){
		try {
			Class.forName("com.mysql.jdbc.Driver");
			final Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=utf-8","root","123456");
			//对con创建代理对象
			Connection proxy = (Connection)Proxy.newProxyInstance(
					con.getClass().getClassLoader(), 
					/*con.getClass().getInterfaces(),*/  //当目标对象是一个具体的类时
					new Class[]{Connection.class},   //目标对象实现的接口
					new InvocationHandler() {
						
						@Override
						public Object invoke(Object proxy, Method method, Object[] args)
								throws Throwable {
							//方法返回值
							Object result = null;
							//当前执行的方法名
							String methodName = method.getName();
							//判断是否执行了close方法的时候，把连接放到连接池
							if("close".equals(methodName)){
								System.out.println("begin:当前执行了close方法");
								//连接放入连接池
								pool.addLast(con);
								System.out.println("end:当前连接已经放入连接池");
							}else{
							//调用目标对象方法
							method.invoke(con, args);
							}
							return result;
						}
					}
					);
			return proxy;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw new RuntimeException(e);
		}
		
	}
	
	
	//3.获取连接
	public  Connection getConnection(){
		//3.1判断连接池中是否有链接，如果有连接，则直接用
		if(pool.size()>0){
			return pool.removeFirst();
		}
		
		//3.2连接池中没有连接，判断，如果没有达到最大连接数，创建
		if(current_count<max_count){
			//记录当前使用的连接数
			current_count++;
			return createConnection();
		}
		//3.3如果当前已经达到最大连接数，抛出异常
		throw new RuntimeException("当前已经达到最大连接数！");
		
	}
	
	//4.释放连接
	public void realeaseConnection(Connection con){
		//4.1 判断：池的数目如果小于初始化练级，就放入池中
		if(pool.size()<init_count){
			pool.addLast(con);
		}else{
			//关闭
			try {
				current_count-- ;
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}	
		
	}
	
	public static void main(String[] args) throws SQLException {
		Pool pool = new Pool();
		System.out.println("当前连接"+pool.current_count);
		
		//使用连接
		pool.getConnection();
		pool.getConnection();
		pool.getConnection();
		pool.getConnection();
		pool.getConnection();
	
		Connection con1 = pool.getConnection();
		con1.close();//如果没用动态代理，连接就关闭了
		//希望：当关闭连接的时候，要把连接放入连接池。（当调用Connetion接口的close方法时候，希望触发pool.addLast(con)操作）
		//解决1:实现Connection接口，重写close方法（方法太多，不太现实）
		//解决2：
		pool.getConnection();
		System.out.println("连接池"+pool.pool.size());
		System.out.println("当前连接"+pool.current_count);
	}
}	
