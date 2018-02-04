package renren.wawabox.bean;

import java.io.Serializable;
import java.util.List;

/**
 * Created by xunwang on 2017/9/19.
 */

public class LiveCheckResultDto implements Serializable {

    private List<LiveCheckOutputDto> output;

    public List<LiveCheckOutputDto> getOutput() {
        return output;
    }

    public void setOutput(List<LiveCheckOutputDto> output) {
        this.output = output;
    }

    public static class LiveCheckOutputDto implements Serializable {

        private int status = -1;

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }
    }
}
