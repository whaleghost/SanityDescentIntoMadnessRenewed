package whaleghost.sanitydimr.capability;

import whaleghost.sanitydimr.ICompoundTagSerializable;

public interface ISanity extends ICompoundTagSerializable
{
    float getSanity();

    void setSanity(float value);
}