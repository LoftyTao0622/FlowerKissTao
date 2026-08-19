package com.zyt.flowerkisstao.knowledge.domain.model;

/**
 * 文章状态。方案原文："文章由管理员编辑、分类、打标签并审核发布。"
 *
 * <p>数值存进 {@code knowledge_article.status}，改了会与存量数据错位。
 */
public enum ArticleStatus {

    /** 草稿。作者在写，公开接口一律查不到 */
    DRAFT(0, "草稿"),

    /** 待审核。已提交，等有发布权限的人过目 */
    PENDING(1, "待审核"),

    /** 已发布。游客也能读 */
    PUBLISHED(2, "已发布"),

    /** 已下架。内容过时或有误时撤下，改完可以重新提交 */
    OFFLINE(3, "已下架");

    private final int code;
    private final String label;

    ArticleStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int code() {
        return code;
    }

    public String label() {
        return label;
    }

    /** 数据库里的值转回枚举。未知值直接抛，比静默当成草稿安全 */
    public static ArticleStatus of(Integer code) {
        if (code != null) {
            for (ArticleStatus status : values()) {
                if (status.code == code) {
                    return status;
                }
            }
        }
        throw new IllegalArgumentException("未知的文章状态：" + code);
    }

    /** 只有已发布的文章对外可见 */
    public boolean isPublic() {
        return this == PUBLISHED;
    }
}
