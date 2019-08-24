public class TypedArgumentListNode implements BaseNode {
    private TypedArgumentNode[] positionArgs;
    private TypedArgumentNode[] normalArgs;
    private TypedArgumentNode[] nameArgs;

    public TypedArgumentListNode(TypedArgumentNode... args) {
        this.normalArgs = args;
    }

    public TypedArgumentListNode(TypedArgumentNode[] positionArgs, TypedArgumentNode[] normalArgs, TypedArgumentNode[] nameArgs) {
        this.normalArgs = normalArgs;
        this.positionArgs = positionArgs;
        this.nameArgs = nameArgs;
    }

    public TypedArgumentNode[] getPositionArgs() {
        return positionArgs;
    }

    public TypedArgumentNode[] getArgs() {
        return normalArgs;
    }

    public TypedArgumentNode[] getNameArgs() {
        return nameArgs;
    }

    public TypedArgumentNode get(int index) {
        return normalArgs[index];
    }
}
