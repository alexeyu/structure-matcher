package nl.alexeyu.structmatcher.json;

/** Nested structure for {@link SampleStructure}; its boolean component yields the path Sub.Flag. */
public class SampleSub {

    private final boolean flag;

    public SampleSub(boolean flag) {
        this.flag = flag;
    }

    public boolean isFlag() {
        return flag;
    }

}
