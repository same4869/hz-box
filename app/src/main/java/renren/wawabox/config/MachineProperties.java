package renren.wawabox.config;

/**
 * Created by xunwang on 2017/9/13.
 */

public class MachineProperties {

    /**
     * # 娃娃机初始位置:
     * //    # 0 = backward, left
     * //    # 1 = forward, left
     * //    # 2 = backward, right
     * //    # 3 = forward, right
     * //            mac_init_loc=0
     * <p>
     * read from properties / server
     */
    public static int MACHINE_INIT_LOC = 0;

    /**
     * @不规范; TODO 等娃娃机天车厘定之后,重新写方案
     */
    public static int MACHINE_BACK_FRONT = 0;

    /**
     * 下爪,到底100抓力抓起来后,多久松爪
     */
    public static int CLAW_TO_FAKE_TIME = 300;

    /**
     * 松爪多久后恢复抓力
     */
    public static int CLAW_FAKE_TIME = 400;
}
