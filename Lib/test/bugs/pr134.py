# test for PR#134 -- int('') and long('') should raise ValueErrors

for func in (int, int, float):
    try:
        func('')
    except ValueError:
        pass
    else:
        print('function', func, 'with empty input should raise ValueError')
