import torch
import torch.nn as nn
import torch.nn.functional as F
import torch as th

class MSELoss(nn.Module):
    def __init__(self, model, weight=None, **kwargs):
        super().__init__()

    def forward(self, input, target):
        return F.mse_loss(input.view(-1), target.view(-1), reduction='mean')


class QLoss(nn.Module):
    def __init__(self, model, weight=None, min_val=1e-3, penalty_negative=1e2, **kwargs):
        self.min_val = min_val
        self.penalty_negative = penalty_negative
        super().__init__()

    def forward(self, input, target):
        # better implement this in a vectorized way
        qerror = []
        for i in range(len(target)):
            # penalty for negative/too small estimates
            if (input[i] < self.min_val).cpu().data.numpy():
            #    # influence on loss for a negative estimate is >= penalty_negative constant
                q_err = (1 - input[i]) * self.penalty_negative
            # otherwise normal q error
            else:
                if (input[i] > target[i]).cpu().data.numpy():
                    q_err = th.abs(input[i]) / th.abs(target[i])
                else:
                    q_err = th.abs(target[i])/ th.abs(input[i])
            qerror.append(q_err)
        loss = torch.mean(torch.cat(qerror))

        return loss
