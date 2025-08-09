package mod.pbj.util;

import com.eliotlash.mclib.math.IValue;
import com.eliotlash.mclib.math.functions.Function;

public final class MolangFunctions {
    public static class BounceEasing extends Function {

        public BounceEasing(IValue[] values, String name) throws Exception {
            super(values, name);
        }

        @Override
        public int getRequiredArguments() {
            return 1;
        }

        @Override
        public double get() {
            var x = Math.pow(this.getArg(0), 3);
            final double c4 = (2 * Math.PI) / 3;

            return x == 0 ? 0 :
                    x == 1 ? 1 :
                            Math.pow(2, -10 * x) * Math.sin((x * 10 - 0.75) * c4) + 1;
        }
    }

    public static class OutBackEasing extends Function {

        public OutBackEasing(IValue[] values, String name) throws Exception {
            super(values, name);
        }

        @Override
        public int getRequiredArguments() {
            return 1;
        }

        @Override
        public double get() {
            var x = this.getArg(0);
            var c1 = 1.70158;
            var c3 = c1 + 1;

            return 1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2);
        }
    }
    public static class InOutQuart extends Function {

        public InOutQuart(IValue[] values, String name) throws Exception {
            super(values, name);
        }

        @Override
        public int getRequiredArguments() {
            return 1;
        }

        @Override
        public double get() {
            var x = this.getArg(0);
            return x < 0.5 ? 8 * x * x * x * x : 1 - Math.pow(-2 * x + 2, 4) / 2;
        }
    }
}
