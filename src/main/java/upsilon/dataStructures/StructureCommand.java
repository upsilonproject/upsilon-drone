package upsilon.dataStructures;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import upsilon.configuration.XmlNodeHelper;

@XmlRootElement
public class StructureCommand extends ConfigStructure {
	private static transient final Logger LOG = LoggerFactory.getLogger(StructureCommand.class);

	public static Vector<String> parseCallCommandArguments(final String fullCommandLine) {
		final String components[] = fullCommandLine.split("(?<!\\\\)!");

		for (int i = 0; i < components.length; i++) {
			components[i] = components[i].replace("\\!", "!");
		}

		final Vector<String> v = new Vector<String>(Arrays.asList(components));

		final Iterator<String> it = v.iterator();

		while (it.hasNext()) {
			if (it.next().isEmpty()) {
				it.remove();
			}
		}

		return v;
	}

	public static String parseCallCommandExecutable(final String parameterValue) {
		if (parameterValue.contains("!")) {
			final String components[] = parameterValue.split("!");

			return components[0];
		} else {
			return parameterValue;
		}
	}

	public static List<String> parseCommandArguments(final String fullOriginalCommandLine) {
		final String fullParsedCommandLine = fullOriginalCommandLine.replace(StructureCommand.parseCommandExecutable(fullOriginalCommandLine), "");

		if (fullParsedCommandLine.contains("!")) {
			throw new IllegalArgumentException("Command definition for arguments cannot contain !, is this a service command line?");
		}

		final Vector<String> args = new Vector<String>();

		for (String arg : fullParsedCommandLine.split(" ")) {
			arg = arg.trim();

			if (!arg.isEmpty()) {
				args.add(arg);
			}
		}

		return args;
	}

	private static String parseCommandArgumentVariable(final String originalArgument, final AbstractService service, final Map<String, String> callingArguments) {
		if (callingArguments.containsKey(originalArgument)) {
			return callingArguments.get(originalArgument);
		} else {
			return originalArgument;
		}
	}

	public static String parseCommandExecutable(final String fullCommandLine) {
		final Pattern patCommandLineExecutable = Pattern.compile("([\\/\\w\\.\\_]+)");
		final Matcher matCommandLineExecutable = patCommandLineExecutable.matcher(fullCommandLine);
		matCommandLineExecutable.find();

		if (matCommandLineExecutable.groupCount() == 0) {
			StructureCommand.LOG.warn("Parsing executable: " + fullCommandLine + ", group count was: " + matCommandLineExecutable.groupCount());
			return "";
		} else {
			return matCommandLineExecutable.group(1).trim();
		}
	}

	private String executable = "???";

	private String name;
	private List<String> definedArguments = new Vector<String>();

	@XmlElement(name = "argument")
	@XmlElementWrapper(name = "arguments")
	public List<String> getArguments() {
		return this.definedArguments;
	}

	@XmlElement
	public String getExecutable() {
		return this.executable;
	}

	public String[] getFinalCommandLinePieces(final StructureService service) {
		final Vector<String> pieces = new Vector<String>();
		pieces.add(this.getExecutable());

		final HashMap<String, String> callingArguments = service.getArguments();

		for (final String arg : this.definedArguments) {
			pieces.add(StructureCommand.parseCommandArgumentVariable(arg, service, callingArguments));
		}

		return pieces.toArray(new String[] {});
	}

	@Override
	@XmlElement
	public String getIdentifier() {
		return this.getName();
	}

	@XmlElement
	public String getName() {
		return this.name;
	}

	public void setCommandLine(final String fullCommandLine) {
		this.executable = StructureCommand.parseCommandExecutable(fullCommandLine);
		this.definedArguments = StructureCommand.parseCommandArguments(fullCommandLine);
	}

	public void setIdentifier(final String string) {
		this.name = string;
	}

	public void setName(final String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Command, executable: " + this.getExecutable();
	}

	@Override
	public void update(final XmlNodeHelper el) {
		this.setIdentifier(el.getAttributeValueUnchecked("id"));
		this.setCommandLine(el.getAttributeValueUnchecked("exec"));
	}
}
