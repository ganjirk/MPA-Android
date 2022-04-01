import kivy
from kivymd.app import MDApp
from kivymd.uix.screen import MDScreen
from kivy.uix.gridlayout import GridLayout
from kivy.uix.label import Label
from kivymd.uix.label import MDLabel
from kivy.uix.image import Image
from kivy.uix.button import Button
from kivymd.uix.button import MDFillRoundFlatButton, MDFillRoundFlatIconButton
from kivy.uix.textinput import TextInput
from kivymd.uix.toolbar import MDToolbar
from kivymd.theming import ThemeManager
import random



class outerLayout(GridLayout):
    
    
    def reset1(self, args):
        self.x = random.randint(1, 50)
        # print(self.x)
        return self.x
    
    def check(self, args):    
        
        try:            
            val = int(self.input_num.text)
            
            if val == self.x:
                self.guess_msg.text = f" {val} is Correct"          
                self.x = random.randint(1, 50)
            elif val < 1:
                self.guess_msg.text = "Enter between 1 to 50"
            elif val > self.x:
                self.guess_msg.text = "Hint: Enter Lower Number"
            else:
                self.guess_msg.text = "Hint: Enter Higher Number"
           
        except:
            self.guess_msg.text = "Only Numbers Allowed !!!"
        

    def __init__(self, **kwargs):       
        
        # call grid layout constructor
        super(outerLayout, self).__init__(**kwargs)
        
        self.x = random.randint(1, 50)
        self.cols = 1
        
        # ToolBar
        self.toolbar = MDToolbar(title = "NumberGuess")
        self.toolbar.pos_hint = {"top":1}
        size_hint_x = None
        size_hint_y = None
        self.toolbar.right_action_items = [["home", lambda x: self.press()]]
        
        # Add toolbar to sceen
        self.add_widget (self.toolbar)
        # Add logo  to screen
        self.add_widget (Image(source="static/logo.jpg",pos_hint = {"center_x":0.5,"center_y":0.7}))
        self.headerText = Label (text = "Enter Any number between 1 to 50", color = 'brown', font_size=20)
        self.add_widget(self.headerText)
       
        # Create new layout with 2 cols
        self.dataLayout = GridLayout()
        self.dataLayout.cols = 2
        # self.dataLayout.size_hint_x =  None
        # self.dataLayout.width = 400
      
        # Add wigets to screen
        self.label1 = MDLabel(text="Enter Number: ", 
                # halign='center', 
                theme_text_color= "Custom",
                text_color= (0, 0, 1, 1),
                # pos_hint = {"center_x":1, "center_y":0.25},
                # theme_text_color = "Primary",
                font_style = "H5")
        self.dataLayout.add_widget(self.label1)                                            

        self.input_num = TextInput(multiline=False, 
        # pos_hint = {"center_x":1, "center_y":0.25}, 
        hint_text = "Enter Any Number")
        self.dataLayout.add_widget(self.input_num)

        self.label2 = MDLabel(width=20)
        self.dataLayout.add_widget(self.label2) 
        self.label3 = MDLabel(width=20)
        self.dataLayout.add_widget(self.label3) 

        # Add Submit Button
        self.dataLayout.button1 = MDFillRoundFlatButton(text="Submit", 
        pos_hint = {"center_x":1, "center_y":0.25}, 
        on_press=self.check)
        self.dataLayout.add_widget(self.dataLayout.button1)

		# Add Reset Button
        self.dataLayout.button2 = MDFillRoundFlatButton(text="Reset",  
        # pos_hint = {"center_x":1, "center_y":0.25}, 
        on_press=self.reset1)
        self.dataLayout.add_widget(self.dataLayout.button2)


        # Add dataLayout to screen
        self.add_widget(self.dataLayout)
        
        
        self.guess_msg = MDLabel(font_style = "H5")
        self.add_widget(self.guess_msg)

    def press(self):
        print("pressed")
    


class NumGuessApp(MDApp):

    def build(self):
        self.theme_cls.primary_palette = "Teal"
        return outerLayout()
        
               
if __name__ == '__main__':
    NumGuessApp().run()