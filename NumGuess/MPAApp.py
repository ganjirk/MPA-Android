import kivy
from kivymd.app import MDApp
from kivymd.uix.screen import MDScreen
from kivy.uix.gridlayout import GridLayout
from kivymd.uix.boxlayout import MDBoxLayout
from kivy.uix.label import Label
from kivymd.uix.label import MDLabel
from kivy.uix.image import Image
from kivymd.uix.button import MDFillRoundFlatButton, MDFillRoundFlatIconButton
from kivy.uix.textinput import TextInput
from kivymd.uix.toolbar import MDToolbar
from kivy.lang import Builder
import random
from kivy.uix.screenmanager import ScreenManager, Screen
from kivy.properties import StringProperty
from kivy.uix.widget import Widget
from kivymd.uix.textfield import MDTextField


# kv=Builder.load_file("mpa.kv")

rand_num = random.randint(1, 50)
val = ''

class MPA_Main(Screen):
    pass

class ScreenManager(ScreenManager):
    pass


class NumberGuess(Screen):
    
    def check(self):    
        global rand_num, val 
        try:              
            
            val = self.ids.input_num.text  
            val = int(val) 
            if val == rand_num:
                guess_msg = f"Number: {val} is Correct Guess"
                self.ids.guess_msg.text = guess_msg
                self.ids.sub_button.disabled = True
                # rand_num = random.randint(1, 50)
                
            elif val < 1:
                guess_msg = "Enter between 1 to 50"
                self.ids.guess_msg.text = guess_msg
            elif val > rand_num:
                guess_msg = "Hint: Enter Lower Number"  
                self.ids.guess_msg.text = guess_msg                
            
            else:
                guess_msg = "Hint: Enter Higher Number"
                self.ids.guess_msg.text = guess_msg                             
            
        except:
            guess_msg = "Only Numbers Allowed !!!"
            self.ids.guess_msg.text = guess_msg
    
    def reset1(self):

        rand_num = random.randint(1, 50)
        guess_msg = ""
        self.ids.guess_msg.text = guess_msg
        self.ids.sub_button.disabled = False
        rand_num = random.randint(1, 50)
        

    LabelText = StringProperty('')


class Converter(Screen):

    state = 0 #initial state
    def flip(self):
        # a method for the "flip" icon
        # changes the state of the app
        if self.state == 0:
            self.state = 1
            self.ids.convert_toolbar.title = "Decimal to Binary"
            self.ids.input_bin.text = "enter a decimal number"
        else:
            self.state = 0
            self.ids.convert_toolbar.title = "Binary to Decimal"
            self.ids.input_bin.text = "enter a binary number"
        # hide labels until needed
        self.ids.prim_label.text = ""
        self.ids.sec_label.text = ""

    def call(self, instance):
        if instance.icon == 'rotate-3d-variant':
            Converter.flip(self)
        elif instance.icon == 'home':
            self.manager.get_screen('MPA_Main').ids.MPA_Main

    def convert(self):
        # a method to find the decimal/binary equivallent
        try:
            if "." not in self.ids.input_bin.text:
                # if the user-provided number is not a fraction
                if self.state == 0:
                    # binary to decimal
                    val = str(int(self.ids.input_bin.text,2))
                    self.ids.sec_label.text = "in decimal is:"
                else:
                    # decimal to binary
                    val = bin(int(self.ids.input_bin.text))[2:]
                    self.ids.sec_label.text = "in binary is:"
                self.ids.prim_label.text = val
            else:
                #if the user provided number is a fraction
                whole, fract = self.ids.input_bin.text.split(".")

                if self.state == 0:
                    #convert binary to decimal
                    whole = int(whole, 2)
                    floating = 0
                    for idx, digit in enumerate(fract):
                        floating += int(digit)*2**(-(idx+1))
                    self.ids.sec_label.text = "in decimal is:"
                    self.ids.prim_label.text = str(whole + floating)
                else:
                    #convert decimal to binary
                    decimal_places = 10
                    whole = bin(int(whole))[2:]
                    fract = float("0."+fract)
                    floating = []
                    for i in range(decimal_places):
                        if fract*2 < 1:
                            floating.append("0")
                            fract *= 2
                        elif fract*2 > 1:
                            floating.append("1")
                            fract = fract*2 - 1
                        elif fract*2 == 1.0:
                            floating.append("1")
                            break
                    self.ids.sec_label.text = "in binary is:"
                    self.ids.prim_label.text = whole + "." + "".join(floating)
        except ValueError:
            #if the user-provided value is invalid
            self.ids.prim_label.text = ""
            if self.state == 0:
                #binary to decimal
                self.ids.sec_label.text = "please enter a valid binary number"
            else:
                #decimal to binary
                self.ids.sec_label.text = "please enter a valid decimal number"



class MPAApp(MDApp):

    def build(self):
        kv = Builder.load_file('mpa.kv')
        
        return kv
    
    guesses_left = 5
    guess_text = 'Guesses Remaining: 5'
    
    


if __name__ == '__main__':
    MPAApp().run()