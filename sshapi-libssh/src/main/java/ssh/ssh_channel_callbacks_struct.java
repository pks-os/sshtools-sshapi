package ssh;
import com.ochafik.lang.jnaerator.runtime.NativeSize;
import com.ochafik.lang.jnaerator.runtime.Structure;
import com.sun.jna.Pointer;
import ssh.SshLibrary.ssh_channel_close_callback;
import ssh.SshLibrary.ssh_channel_data_callback;
import ssh.SshLibrary.ssh_channel_eof_callback;
import ssh.SshLibrary.ssh_channel_exit_signal_callback;
import ssh.SshLibrary.ssh_channel_exit_status_callback;
import ssh.SshLibrary.ssh_channel_signal_callback;
/**
 * <i>native declaration : /usr/include/libssh/callbacks.h:350</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.free.fr/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class ssh_channel_callbacks_struct extends Structure<ssh_channel_callbacks_struct, ssh_channel_callbacks_struct.ByValue, ssh_channel_callbacks_struct.ByReference > {
	/// DON'T SET THIS use ssh_callbacks_init() instead.
	public NativeSize size;
	/**
	 * User-provided data. User is free to set anything he wants here<br>
	 * C type : void*
	 */
	public Pointer userdata;
	/**
	 * This functions will be called when there is data available.<br>
	 * C type : ssh_channel_data_callback
	 */
	public ssh_channel_data_callback channel_data_function;
	/**
	 * This functions will be called when the channel has received an EOF.<br>
	 * C type : ssh_channel_eof_callback
	 */
	public ssh_channel_eof_callback channel_eof_function;
	/**
	 * This functions will be called when the channel has been closed by remote<br>
	 * C type : ssh_channel_close_callback
	 */
	public ssh_channel_close_callback channel_close_function;
	/**
	 * This functions will be called when a signal has been received<br>
	 * C type : ssh_channel_signal_callback
	 */
	public ssh_channel_signal_callback channel_signal_function;
	/**
	 * This functions will be called when an exit status has been received<br>
	 * C type : ssh_channel_exit_status_callback
	 */
	public ssh_channel_exit_status_callback channel_exit_status_function;
	/**
	 * This functions will be called when an exit signal has been received<br>
	 * C type : ssh_channel_exit_signal_callback
	 */
	public ssh_channel_exit_signal_callback channel_exit_signal_function;
	public ssh_channel_callbacks_struct() {
		super();
		initFieldOrder();
	}
	protected void initFieldOrder() {
		setFieldOrder(new java.lang.String[]{"size", "userdata", "channel_data_function", "channel_eof_function", "channel_close_function", "channel_signal_function", "channel_exit_status_function", "channel_exit_signal_function"});
	}
	/**
	 * @param size DON'T SET THIS use ssh_callbacks_init() instead.<br>
	 * @param userdata User-provided data. User is free to set anything he wants here<br>
	 * C type : void*<br>
	 * @param channel_data_function This functions will be called when there is data available.<br>
	 * C type : ssh_channel_data_callback<br>
	 * @param channel_eof_function This functions will be called when the channel has received an EOF.<br>
	 * C type : ssh_channel_eof_callback<br>
	 * @param channel_close_function This functions will be called when the channel has been closed by remote<br>
	 * C type : ssh_channel_close_callback<br>
	 * @param channel_signal_function This functions will be called when a signal has been received<br>
	 * C type : ssh_channel_signal_callback<br>
	 * @param channel_exit_status_function This functions will be called when an exit status has been received<br>
	 * C type : ssh_channel_exit_status_callback<br>
	 * @param channel_exit_signal_function This functions will be called when an exit signal has been received<br>
	 * C type : ssh_channel_exit_signal_callback
	 */
	public ssh_channel_callbacks_struct(NativeSize size, Pointer userdata, ssh_channel_data_callback channel_data_function, ssh_channel_eof_callback channel_eof_function, ssh_channel_close_callback channel_close_function, ssh_channel_signal_callback channel_signal_function, ssh_channel_exit_status_callback channel_exit_status_function, ssh_channel_exit_signal_callback channel_exit_signal_function) {
		super();
		this.size = size;
		this.userdata = userdata;
		this.channel_data_function = channel_data_function;
		this.channel_eof_function = channel_eof_function;
		this.channel_close_function = channel_close_function;
		this.channel_signal_function = channel_signal_function;
		this.channel_exit_status_function = channel_exit_status_function;
		this.channel_exit_signal_function = channel_exit_signal_function;
		initFieldOrder();
	}
	@Override
	protected ByReference newByReference() { return new ByReference(); }
	@Override
	protected ByValue newByValue() { return new ByValue(); }
	@Override
	protected ssh_channel_callbacks_struct newInstance() { return new ssh_channel_callbacks_struct(); }
	public static ssh_channel_callbacks_struct[] newArray(int arrayLength) {
		return Structure.newArray(ssh_channel_callbacks_struct.class, arrayLength);
	}
	public static class ByReference extends ssh_channel_callbacks_struct implements Structure.ByReference {
		
	};
	public static class ByValue extends ssh_channel_callbacks_struct implements Structure.ByValue {
		
	};
}
