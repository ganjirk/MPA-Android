from cgitb import text
from multiprocessing import context
from django.shortcuts import render

Millimeter = 0
Centimetre = 0
Metre = 0
Kilometer = 0
Mile = 0
Inch = 0
text1=0

convert_to_metre = {
    "Millimeter": 0.001,
    "Centimetre": 0.01,
    "Metre": 1.0,
    "Kilometer": 1000,
    "Mile": 1609.34,
    "Inch": 0.0254
}
convert_from_metre = {
    "Millimeter": 1000,
    "Centimetre": 100,
    "Metre": 1.0,
    "Kilometer": 0.001,
    "Mile": 0.000621371,
    "Inch": 39.3701
}


def LenCon(inp):
    global to_meter, result, Millimeter, Centimetre, Metre, Kilometer, Mile, Inch, text1
    if inp.method == 'POST'  and inp.POST.get('text1', 0):
        text1 = float(inp.POST['text1'])

        if text1 == 0:
            Millimeter = 0
            Centimetre = 0
            Metre = 0
            Kilometer = 0
            Mile = 0
            Inch = 0
            text1=0

        elif text1!=0:
            
            Millimeter = text1 * convert_from_metre["Millimeter"]
            Centimetre = text1 * convert_from_metre["Centimetre"]
            Metre = text1 * convert_from_metre["Metre"]
            Kilometer = text1 * convert_from_metre["Kilometer"]
            Mile = text1 * convert_from_metre["Mile"]
            Inch = text1 * round(convert_from_metre["Inch"],2)
            
            
    else:
        if inp.method == 'POST'  and inp.POST.get('text2', 0):

            if text2 == 0:

                Millimeter = 0
                Centimetre = 0
                Metre = 0
                Kilometer = 0
                Mile = 0
                Inch = 0
                text2=0
            
            else:

                text2 = float(inp.POST['text2'])
                
                text2 = text2 * convert_to_metre["Metre"] 
                
                Millimeter = text2 * convert_from_metre["Millimeter"]
                Centimetre = text2 * convert_from_metre["Centimetre"]

                Metre = text2 * convert_to_metre["Metre"]
                Kilometer = text2 * convert_to_metre["Kilometer"]
                Mile = text2 * convert_to_metre["Mile"]
                Inch = text2 * convert_to_metre["Inch"]

        
    context = {'Millimeter': Millimeter, 'Centimetre': Centimetre, 'Metre':Metre, 'Kilometer':Kilometer, 
    'Mile':Mile, 'Inch':Inch}
    return render(inp, "lencon.html", context)

