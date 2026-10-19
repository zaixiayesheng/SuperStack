package com.zaixiayesheng.superstack;

/**
 * 按住 Shift 点击合成结果时，原版会一直循环到背包放不下为止。
 * 大堆叠下一格就有三万多个，这个循环会同步跑上万次，客户端直接卡死、联机时服务端 tick 超时。
 *
 * <p>这里只做一件事：给「一次点击」里的合成次数封顶。计数在 {@code AbstractContainerMenu#clicked}
 * 进入时清零，合成菜单每次真正处理一次合成就加一，到顶后让 {@code quickMoveStack} 返回空，
 * 外层循环自然结束——结果格和材料都保持原样，不会丢东西，玩家再点一次继续。
 */
public final class SuperStackCraftLimiter {

    private static int crafts;

    private SuperStackCraftLimiter() {
    }

    /** 每次点击开始时调用。 */
    public static void reset() {
        crafts = 0;
    }

    /**
     * 报告一次合成尝试。
     *
     * @return true 表示本次点击已经到达上限，调用方应当停止继续合成
     */
    public static boolean reachedLimit() {
        int limit = SuperStackConfig.maxCraftsPerClick();
        if (crafts >= limit) {
            return true;
        }
        crafts++;
        return false;
    }
}
