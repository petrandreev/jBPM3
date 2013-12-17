public class AmountUpdate implements ActionHandler {
  public void execute(ExecutionContext ctx) throws Exception {
    // business logic
    Float erpAmount = ...get amount from erp-system...;
    Float processAmount = (Float) ctx.getContextInstance().getVariable("amount");
    float result = erpAmount.floatValue() + processAmount.floatValue();
    ...update erp-system with the result...;
    
    // graph execution propagation
    if (result > 5000) {
      ctx.leaveNode(ctx, "big amounts");
    } else {
      ctx.leaveNode(ctx, "small amounts");
    }
  }
}