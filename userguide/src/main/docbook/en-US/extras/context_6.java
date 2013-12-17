public interface Converter extends Serializable {
	boolean supports(Object value);
	Object convert(Object o);
	Object revert(Object o);
}