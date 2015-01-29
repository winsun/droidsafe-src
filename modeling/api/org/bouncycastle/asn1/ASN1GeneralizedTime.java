package org.bouncycastle.asn1;

// Droidsafe Imports
import droidsafe.runtime.*;
import droidsafe.helpers.*;
import droidsafe.annotations.*;
import java.util.Date;

public class ASN1GeneralizedTime extends DERGeneralizedTime {
    @DSComment("Package priviledge")
    @DSBan(DSCat.DEFAULT_MODIFIER)
    @DSGenerator(tool_name = "Doppelganger", tool_version = "2.0", generated_on = "2013-12-30 13:00:12.936 -0500", hash_original_method = "E338472F0350A218212107AE966181F8", hash_generated_method = "E338472F0350A218212107AE966181F8")
    
ASN1GeneralizedTime(byte[] bytes)
    {
        super(bytes);
    }

    @DSGenerator(tool_name = "Doppelganger", tool_version = "2.0", generated_on = "2013-12-30 13:00:12.939 -0500", hash_original_method = "D40D912D80E165C0BE3CEE0F9A088081", hash_generated_method = "47AE14D6B6273C6491EAA29643CD7069")
    
public ASN1GeneralizedTime(Date time)
    {
        super(time);
    }

    @DSGenerator(tool_name = "Doppelganger", tool_version = "2.0", generated_on = "2013-12-30 13:00:12.943 -0500", hash_original_method = "43E98C712F213D3EB238CD108159421F", hash_generated_method = "F29C99A366F10D9EC5E6BAB276A9A9B6")
    
public ASN1GeneralizedTime(String time)
    {
        super(time);
    }
    
}
