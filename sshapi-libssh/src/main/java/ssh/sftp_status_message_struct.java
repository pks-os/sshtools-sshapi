package ssh;
import java.util.Arrays;
import java.util.List;

import com.ochafik.lang.jnaerator.runtime.Structure;
import com.sun.jna.Pointer;
import ssh.SshLibrary.ssh_string;
/**
 * SSH_FXP_MESSAGE described into .7 page 26<br>
 * <i>native declaration : /usr/include/libssh/sftp.h:142</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.free.fr/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class sftp_status_message_struct extends Structure<sftp_status_message_struct, sftp_status_message_struct.ByValue, sftp_status_message_struct.ByReference > {
	public int id;
	public int status;
	/// C type : ssh_string
	public ssh_string error;
	/// C type : ssh_string
	public ssh_string lang;
	/// C type : char*
	public Pointer errormsg;
	/// C type : char*
	public Pointer langmsg;
	public sftp_status_message_struct() {
		super();
	}
	@Override
	protected List<String> getFieldOrder() {
		return Arrays.asList(new java.lang.String[]{"id", "status", "error", "lang", "errormsg", "langmsg"});
	}
	/**
	 * @param error C type : ssh_string<br>
	 * @param lang C type : ssh_string<br>
	 * @param errormsg C type : char*<br>
	 * @param langmsg C type : char*
	 */
	public sftp_status_message_struct(int id, int status, ssh_string error, ssh_string lang, Pointer errormsg, Pointer langmsg) {
		super();
		this.id = id;
		this.status = status;
		this.error = error;
		this.lang = lang;
		this.errormsg = errormsg;
		this.langmsg = langmsg;
	}
	@Override
	protected ByReference newByReference() { return new ByReference(); }
	@Override
	protected ByValue newByValue() { return new ByValue(); }
	@Override
	protected sftp_status_message_struct newInstance() { return new sftp_status_message_struct(); }
	public static sftp_status_message_struct[] newArray(int arrayLength) {
		return Structure.newArray(sftp_status_message_struct.class, arrayLength);
	}
	public static class ByReference extends sftp_status_message_struct implements Structure.ByReference {
		
	};
	public static class ByValue extends sftp_status_message_struct implements Structure.ByValue {
		
	};
}
