package com.isharpever.tool.config.disconf;

import com.baidu.disconf.client.common.annotations.DisconfFile;
import com.baidu.disconf.client.common.annotations.DisconfFileItem;
import com.baidu.disconf.client.common.annotations.DisconfUpdateService;
import com.baidu.disconf.client.common.update.IDisconfUpdate;
import com.isharpever.tool.datasource.routing.ChooseDataSource;
import com.isharpever.tool.datasource.routing.LookupKeyConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/**
 * riskcontroldb切库
 */
@DisconfFile(app = "xxx-common", filename = "transfer-xxxdb.properties", version = "common")
@DisconfUpdateService(classes = {DisconfTransferRiskcontroldbConfig.class})
@Slf4j
public class DisconfTransferRiskcontroldbConfig implements IDisconfUpdate {

    @Autowired
    private Environment env;

    @Autowired
    private ChooseDataSource chooseDataSource;

    private String oldUrlKey;
    private String oldUserNameKey;
    private String oldPasswordKey;
    private String oldUrlSlaveKey;
    private String oldUserNameSlaveKey;
    private String oldPasswordSlaveKey;

    public DisconfTransferRiskcontroldbConfig(String oldUrlKey, String oldUserNameKey, String oldPasswordKey,
            String oldUrlSlaveKey, String oldUserNameSlaveKey, String oldPasswordSlaveKey) {
        this.oldUrlKey = oldUrlKey;
        this.oldUserNameKey = oldUserNameKey;
        this.oldPasswordKey = oldPasswordKey;
        this.oldUrlSlaveKey = oldUrlSlaveKey;
        this.oldUserNameSlaveKey = oldUserNameSlaveKey;
        this.oldPasswordSlaveKey = oldPasswordSlaveKey;
    }

    /**
     * 新库url
     */
    private String newUrl;

    /**
     * 新库username
     */
    private String newUserName;

    /**
     * 新库password
     */
    private String newPassword;

    /**
     * 新库url-从
     */
    private String newUrlSlave;

    /**
     * 新库username-从
     */
    private String newUserNameSlave;

    /**
     * 新库password-从
     */
    private String newPasswordSlave;

    /**
     * 0:旧 1:新
     */
    private int op = -1;

    @DisconfFileItem(name = "new.url")
    public String getNewUrl() {
        return newUrl;
    }

    public void setNewUrl(String newUrl) {
        this.newUrl = newUrl;
    }

    @DisconfFileItem(name = "new.username")
    public String getNewUserName() {
        return newUserName;
    }

    public void setNewUserName(String newUserName) {
        this.newUserName = newUserName;
    }

    @DisconfFileItem(name = "new.password")
    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    @DisconfFileItem(name = "new.url.slave")
    public String getNewUrlSlave() {
        return newUrlSlave;
    }

    public void setNewUrlSlave(String newUrlSlave) {
        this.newUrlSlave = newUrlSlave;
    }

    @DisconfFileItem(name = "new.username.slave")
    public String getNewUserNameSlave() {
        return newUserNameSlave;
    }

    public void setNewUserNameSlave(String newUserNameSlave) {
        this.newUserNameSlave = newUserNameSlave;
    }

    @DisconfFileItem(name = "new.password.slave")
    public String getNewPasswordSlave() {
        return newPasswordSlave;
    }

    public void setNewPasswordSlave(String newPasswordSlave) {
        this.newPasswordSlave = newPasswordSlave;
    }

    @DisconfFileItem(name = "op")
    public int getOp() {
        return op;
    }

    public void setOp(int op) {
        if (this.op == -1) {
            this.op = op;
            return;
        }

        log.info("切库--开始 op={}", op);
        if (this.op == op) {
            log.info("切库--不需要切 this.op={}", this.op);
            return;
        }
        try {
            // 使用旧库配置
            if (op == 0) {
                String url = env.getProperty(this.oldUrlKey);
                String userName = env.getProperty(this.oldUserNameKey);
                String password = env.getProperty(this.oldPasswordKey);
                String urlSlave = env.getProperty(this.oldUrlSlaveKey);
                String userNameSlave = env.getProperty(this.oldUserNameSlaveKey);
                String passwordSlave = env.getProperty(this.oldPasswordSlaveKey);
                log.info("切库--使用旧库配置 url={} urlSlave={}", url, urlSlave);

                chooseDataSource.update(LookupKeyConstant.MASTER, url, userName, password);
                chooseDataSource.update(LookupKeyConstant.SLAVE, urlSlave, userNameSlave, passwordSlave);
            }
            // 使用新库配置
            else {
                if (StringUtils.isNotBlank(this.newUrl) || StringUtils.isNotBlank(this.newUserName)
                        || StringUtils.isNotBlank(this.newPassword)) {
                    log.info("切库--新库配置未加载 op={}", op);
                    return;
                }
                log.info("切库--使用新库配置 url={} urlSlave={}", this.newUrl, this.newUrlSlave);
                chooseDataSource.update(LookupKeyConstant.MASTER, this.newUrl, this.newUserName, this.newPassword);
                chooseDataSource.update(LookupKeyConstant.SLAVE, this.newUrlSlave, this.newUserNameSlave, this.newPasswordSlave);
            }
        } catch (Exception e) {
            log.error("切库异常", e);
            return;
        }
        log.info("切库--成功");
        this.op = op;
    }

    @Override
    public void reload() throws Exception {
    }
}
