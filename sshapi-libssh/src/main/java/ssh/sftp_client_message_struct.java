package ssh;
import com.ochafik.lang.jnaerator.runtime.Structure;
import com.sun.jna.Pointer;
import ssh.SshLibrary.ssh_buffer;
import ssh.SshLibrary.ssh_string;
/**
 * this is a bunch of all data that could be into a message<br>
 * <i>native declaration : /usr/include/libssh/sftp.h:121</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.free.fr/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class sftp_client_message_struct extends Structure<sftp_client_message_struct, sftp_client_message_struct.ByValue, sftp_client_message_struct.ByReference > {
	/// C type : sftp_session
	public ssh.sftp_session_struct.ByReference sftp;
	public byte type;
	public int id;
	/**
	 * can be "path"<br>
	 * C type : char*
	 */
	public Pointer filename;
	public int flags;
	/// C type : sftp_attributes
	public ssh.sftp_attributes_struct.ByReference attr;
	/// C type : ssh_string
	public ssh_string handle;
	public long offset;
	public int len;
	public int attr_num;
	/**
	 * used by sftp_reply_attrs<br>
	 * C type : ssh_buffer
	 */
	public ssh_buffer attrbuf;
	/**
	 * can be newpath of rename()<br>
	 * C type : ssh_string
	 */
	public ssh_string data;
	public sftp_client_message_struct() {
		super();
		initFieldOrder();
	}
	protected void initFieldOrder() {
		setFieldOrder(new java.lang.String[]{"sftp", "type", "id", "filename", "flags", "attr", "handle", "offset", "len", "attr_num", "attrbuf", "data"});
	}
	@Override
	protected ByReference newByReference() { return new ByReference(); }
	@Override
	protected ByValue newByValue() { return new ByValue(); }
	@Override
	protected sftp_client_message_struct newInstance() { return new sftp_client_message_struct(); }
	public static sftp_client_message_struct[] newArray(int arrayLength) {
		return Structure.newArray(sftp_client_message_struct.class, arrayLength);
	}
	public static class ByReference extends sftp_client_message_struct implements Structure.ByReference {
		
	};
	public static class ByValue extends sftp_client_message_struct implements Structure.ByValue {
		
	};
}
