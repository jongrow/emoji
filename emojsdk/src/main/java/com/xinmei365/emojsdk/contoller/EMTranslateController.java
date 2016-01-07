package com.xinmei365.emojsdk.contoller;

import com.xinmei365.emojsdk.domain.Constant;
import com.xinmei365.emojsdk.domain.EMCharacterEntity;
import com.xinmei365.emojsdk.domain.EMTranslatEntity;
import com.xinmei365.emojsdk.notify.INotifyCallback;
import com.xinmei365.emojsdk.notify.NotifyEntity;
import com.xinmei365.emojsdk.notify.NotifyKeys;
import com.xinmei365.emojsdk.notify.NotifyManager;
import com.xinmei365.emojsdk.orm.EMDBMagager;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xinmei on 15/12/14.
 */
public class EMTranslateController implements INotifyCallback {

    private final String TAG = EMTranslateController.class.getSimpleName();
    private static EMTranslateController mInstance;
    private static final char mEmojHolder = '☃';
    private static final char mSpace = ' ';
    private IEMTranslateCallback mEMTranslateCallback;

    private EMTranslateController() {
        registerNotify();
    }

    private void registerNotify() {
        NotifyManager.getInstance().registerNotifyCallback(NotifyKeys.ALL_EMOJ_HAS_DOWNLOAD, this);
    }

    private void unRegisterNotify() {
        NotifyManager.getInstance().removeNotifyCallback(NotifyKeys.ALL_EMOJ_HAS_DOWNLOAD, this);
    }

    public static EMTranslateController getInstance() {
        if (mInstance == null) {
            synchronized (EMTranslateController.class) {
                if (mInstance == null) {
                    mInstance = new EMTranslateController();
                }
            }
        }
        return mInstance;
    }

    /**
     * 翻译用户的输入内容，检索热词，并转换成对应的emoj
     */
    public void translateMsg(CharSequence content,IEMTranslateCallback translateCallback) {
        setEMTranslateCallback(translateCallback);

        ArrayList<EMCharacterEntity> emTransEntries = splitAllContent(content);
        Map<String, ArrayList<EMCharacterEntity>> jonAndTranMap = EMDBMagager.getInstance().filterTranslateWord(emTransEntries);

        ArrayList<EMCharacterEntity> transferArr = jonAndTranMap.get(Constant.KEY_EMOJ_TRANSFER_ASSEMBLE_ARR);
        ArrayList<EMCharacterEntity> joinArr = jonAndTranMap.get(Constant.KEY_EMOJ_ALL_ASSEMBLE_ARR);

        if (transferArr.size() == 0) {
            //TODO:没有需要翻译的emoj key,通知用户更新
            if (mEMTranslateCallback != null) {
                mEMTranslateCallback.onEmptyMsgTranslate();
            }
        } else {
            MessageQueueManager.getInstance().putNeedAssebleKeys(joinArr);
            MessageQueueManager.getInstance().putAllNeedTransEmojKey(transferArr);
        }

    }

    private ArrayList<EMCharacterEntity> splitAllContent(CharSequence content) {
        String regex = "\\b\\w+-\\w+\\b|\\b\\w+\\b";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);

        ArrayList<EMCharacterEntity> wordArrs = new ArrayList<EMCharacterEntity>();
        ArrayList<EMCharacterEntity> allArrs = new ArrayList<EMCharacterEntity>();

        while (matcher.find()) {
            int wordStart = matcher.start();
            int wordEnd = matcher.end();
            CharSequence word = matcher.group();
            EMCharacterEntity transEntry = new EMCharacterEntity(wordStart, wordEnd, word, EMCharacterEntity.CharacterType.Normal);
            wordArrs.add(transEntry);
        }

        for (int i = 0; i < wordArrs.size(); i++) {
            EMCharacterEntity befEntry = wordArrs.get(i);
            EMCharacterEntity afterEntry = null;
            if (i + 1 < wordArrs.size()) {
                afterEntry = wordArrs.get(i + 1);
            }
            //process 第一个单词之前的内容，可能为emoj或者是空格，有可能是多个
            if (i == 0 && befEntry.mWordStart != 0) {
                CharSequence tempStr = content.subSequence(0, befEntry.mWordStart);
                for (int k = 0; k < tempStr.length(); k++) {
                    if (tempStr.charAt(k) == mEmojHolder) {
                        EMCharacterEntity transEntry = new EMCharacterEntity(k, k + 1, tempStr, EMCharacterEntity.CharacterType.Emoj);
                        allArrs.add(transEntry);
                    } else if (tempStr.charAt(k) == mSpace) {
                        EMCharacterEntity transEntry = new EMCharacterEntity(k, k + 1, tempStr, EMCharacterEntity.CharacterType.Space);
                        allArrs.add(transEntry);
                    } else {
                        EMCharacterEntity transEntry = new EMCharacterEntity(k, k + 1, String.valueOf(tempStr.charAt(k)), EMCharacterEntity.CharacterType.Other);
                        allArrs.add(transEntry);
                    }
                }
            }
            //process 之后每个单词之间的内容
            if (afterEntry != null && afterEntry.mWordStart - befEntry.mWordEnd > 0) {
                for (int k = 0; k < afterEntry.mWordStart - befEntry.mWordEnd; k++) {
                    int tempStart = befEntry.mWordEnd + k;
                    int tempEnd = tempStart + 1;
                    CharSequence tempStr = content.subSequence(tempStart, tempEnd);
                    if (tempStr.equals(String.valueOf(mEmojHolder))) {
                        EMCharacterEntity transEntry = new EMCharacterEntity(tempStart, tempEnd, tempStr, EMCharacterEntity.CharacterType.Emoj);
                        allArrs.add(transEntry);
                    } else if (tempStr.equals(String.valueOf(mSpace))) {
                        EMCharacterEntity transEntry = new EMCharacterEntity(tempStart, tempEnd, tempStr, EMCharacterEntity.CharacterType.Space);
                        allArrs.add(transEntry);
                    } else {
                        EMCharacterEntity transEntry = new EMCharacterEntity(tempStart, tempEnd, tempStr, EMCharacterEntity.CharacterType.Other);
                        allArrs.add(transEntry);
                    }
                }
            }

            if (i == wordArrs.size() - 1) {
                if (befEntry.mWordEnd != content.length()) {
                    for (int k = 0; k < content.length() - befEntry.mWordEnd; k++) {
                        int tempStart = befEntry.mWordEnd + k;
                        int tempEnd = tempStart + 1;
                        CharSequence tempStr = content.subSequence(tempStart, tempEnd);
                        if (tempStr.equals(String.valueOf(mEmojHolder))) {
                            EMCharacterEntity transEntry = new EMCharacterEntity(tempStart, tempEnd, tempStr, EMCharacterEntity.CharacterType.Emoj);
                            allArrs.add(transEntry);
                        } else if (tempStr.equals(String.valueOf(mSpace))) {
                            EMCharacterEntity transEntry = new EMCharacterEntity(tempStart, tempEnd, tempStr, EMCharacterEntity.CharacterType.Space);
                            allArrs.add(transEntry);
                        } else {
                            EMCharacterEntity transEntry = new EMCharacterEntity(tempStart, tempEnd, tempStr, EMCharacterEntity.CharacterType.Other);
                            allArrs.add(transEntry);
                        }
                    }
                }
            }
        }

        allArrs.addAll(wordArrs);
        Collections.sort(allArrs, new Comparator<EMCharacterEntity>() {
            @Override
            public int compare(EMCharacterEntity lhs, EMCharacterEntity rhs) {
                if (lhs.mWordStart > rhs.mWordStart) {
                    return 1;
                } else if (lhs.mWordStart < rhs.mWordStart) {
                    return -1;
                }
                return 0;
            }
        });
        return allArrs;
    }


    public void setEMTranslateCallback(IEMTranslateCallback emojTranCallback) {
        this.mEMTranslateCallback = emojTranCallback;
    }


    @Override
    public void notifyCallback(NotifyEntity entity) {
        try {
            String key = entity.getKey();
            if (key.equals(NotifyKeys.ALL_EMOJ_HAS_DOWNLOAD)) {
                //TODO: 所有待翻译的emoj key的图片都已经下载完毕，通知UI去更新，assemble
                ArrayList<EMCharacterEntity> allAssembleArr = (ArrayList<EMCharacterEntity>) entity.getObject();
                if (allAssembleArr.size() > 0 && mEMTranslateCallback != null) {
                    EMTranslatEntity translatEntity = EMAssembleController.getInstance().assembleSpan(allAssembleArr);
                    mEMTranslateCallback.onEmojTransferSuccess(translatEntity);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void destroy() {
        unRegisterNotify();
    }
}
